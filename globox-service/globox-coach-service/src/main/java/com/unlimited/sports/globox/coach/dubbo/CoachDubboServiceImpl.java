package com.unlimited.sports.globox.coach.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.coach.mapper.*;
import com.unlimited.sports.globox.coach.service.ICoachSlotService;
import com.unlimited.sports.globox.common.lock.RedisDistributedLock;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.coach.dto.*;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.coach.entity.CoachCourseType;
import com.unlimited.sports.globox.model.coach.entity.CoachProfile;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotRecord;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotTemplate;
import com.unlimited.sports.globox.model.coach.enums.CoachServiceTypeEnum;
import com.unlimited.sports.globox.dubbo.coach.CoachDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.common.result.CoachErrorCodeEnum.*;


/**
 * 教练RPC服务实现 - 为订单服务提供价格查询功能
 */
@Component
@DubboService(group = "rpc")
@Slf4j
public class CoachDubboServiceImpl implements CoachDubboService {

    @Autowired
    private CoachSlotRecordMapper slotRecordMapper;

    @Autowired
    private CoachSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private CoachProfileMapper coachProfileMapper;

    @Autowired
    private CoachCourseTypeMapper courseTypeMapper;

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private ICoachSlotService coachSlotService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<CoachPricingResultDto> quoteCoach(CoachPricingRequestDto dto) {
        log.info("教练预约价格查询 - userId: {}, coachUserId: {}, slotIds: {}, serviceTypeId: {}",
                dto.getUserId(), dto.getCoachUserId(), dto.getSlotIds(), dto.getServiceTypeId());

        // 参数验证
        if (dto.getSlotIds() == null || dto.getSlotIds().isEmpty()) {
            RpcResult.error(PARAM_SLOT_EMPTY);
        }

        // 查询时段模板
        List<CoachSlotTemplate> templates = slotTemplateMapper.selectBatchIds(dto.getSlotIds());
        if (templates.size() != dto.getSlotIds().size()) {
            RpcResult.error(SLOT_NOT_EXIST);
        }

        // 验证所有时段属于同一教练
        boolean allSameCoach = templates.stream()
                .allMatch(template -> template.getCoachUserId().equals(dto.getCoachUserId()));
        if (!allSameCoach) {
            RpcResult.error(SLOT_COACH_NOT_MATCH);
        }

        // 验证模板是否支持用户选择的服务类型
        validateServiceTypeCompatibility(templates, dto.getServiceTypeId(), dto.getCoachUserId());

        // 第一次验证（获取锁前）- 快速失败
        validateSlots(dto.getSlotIds(), dto.getBookingDate());

        // 构建分布式锁的key（基于模板ID + 日期）
        List<String> lockKeys = dto.getSlotIds().stream()
                .map(slotId -> buildLockKey(slotId, dto.getBookingDate()))
                .collect(Collectors.toList());

        List<RLock> locks = null;

        try {
            // 批量获取所有时段的锁
            locks = redisDistributedLock.tryLockMultiple(lockKeys, 1, -1L, TimeUnit.SECONDS);
            if (locks == null) {
                log.warn("获取分布式锁失败 - userId: {}, slotIds: {}",
                        dto.getUserId(), dto.getSlotIds());
                RpcResult.error(LOCK_ACQUIRE_FAILED);
            }
            log.info("【锁包事务】成功获取分布式锁 - userId: {}, 时段数: {}",
                    dto.getUserId(), dto.getSlotIds().size());

            // 在事务中执行锁定和计价逻辑
            CoachPricingResultDto result = executeBookingInTransaction(dto, templates).getData();

            log.info("事务执行成功 - userId: {}", dto.getUserId());
            return RpcResult.ok(result);

        } finally {
            // 释放锁（无论事务成功还是失败）
            if (locks != null) {
                redisDistributedLock.unlockMultiple(locks);
                log.info("【锁包事务】释放分布式锁 - userId: {}", dto.getUserId());
            }
        }
    }



    /**
     * 在事务中执行预约锁定和计价
     */
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<CoachPricingResultDto> executeBookingInTransaction(
            CoachPricingRequestDto dto,
            List<CoachSlotTemplate> templates) {

        // 第二次验证（事务内）- 确保数据一致性
        validateSlots(dto.getSlotIds(), dto.getBookingDate());

        // 查询教练信息
        CoachProfile coachProfile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );
        if (coachProfile == null) {
            RpcResult.error(COACH_INFO_NOT_EXIST);
        }

        // 查询用户信息
        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(dto.getCoachUserId());
        Assert.rpcResultOk(rpcResult);
        UserInfoVo coachUserInfo = rpcResult.getData();

        // 查询用户选择的课程服务
        CoachCourseType courseType = courseTypeMapper.selectById(dto.getServiceTypeId());
        if (courseType == null || !courseType.getCoachUserId().equals(dto.getCoachUserId())) {
            RpcResult.error(SERVICE_TYPE_UNKNOWN);
        }

        // 获取服务类型描述
        String serviceTypeDesc = "";
        try {
            CoachServiceTypeEnum serviceTypeEnum =
                    CoachServiceTypeEnum.fromValue(courseType.getCoachServiceTypeEnum());
            serviceTypeDesc = serviceTypeEnum.getDescription();
        } catch (Exception e) {
            log.warn("获取服务类型描述失败", e);
        }

        // 按模板ID分组
        Map<Long, CoachSlotTemplate> templateMap = templates.stream()
                .collect(Collectors.toMap(
                        CoachSlotTemplate::getCoachSlotTemplateId,
                        t -> t
                ));

        // 锁定所有时段（按需创建记录）
        List<CoachSlotQuote> slotQuotes = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        // 按时间排序模板
        List<CoachSlotTemplate> sortedTemplates = templates.stream()
                .sorted(Comparator.comparing(CoachSlotTemplate::getStartTime))
                .toList();

        for (CoachSlotTemplate template : sortedTemplates) {
            // 查询或创建记录并锁定
            CoachSlotRecord record = lockSlotByTemplate(
                    template,
                    dto.getBookingDate(),
                    dto.getUserId()
            );

            // 使用课程服务的价格
            BigDecimal slotPrice = courseType.getCoachPrice();

            // 构建时段报价
            CoachSlotQuote quote = CoachSlotQuote.builder()
                    .recordId(record.getCoachSlotRecordId())
                    .coachId(dto.getCoachUserId())
                    .coachName(coachUserInfo.getNickName())
                    .bookingDate(dto.getBookingDate())
                    .startTime(template.getStartTime())
                    .endTime(template.getEndTime())
                    .recordExtras(Collections.emptyList()) // 教练预约暂无额外费用
                    .unitPrice(slotPrice)
                    .slotTemplateId(template.getCoachSlotTemplateId())
                    .durationMinutes(template.getDurationMinutes())
                    .serviceType(courseType.getCoachServiceTypeEnum())
                    .serviceTypeDesc(serviceTypeDesc)
                    .build();

            slotQuotes.add(quote);
            totalPrice = totalPrice.add(slotPrice);
        }

        // 获取第一个模板的可接受区域信息
        CoachSlotTemplate firstTemplate = sortedTemplates.get(0);
        List<String> acceptableAreas = parseAcceptableAreas(firstTemplate.getAcceptableAreas());

        // 构建返回结果
        CoachPricingResultDto resultDto = CoachPricingResultDto.builder()
                .slotQuotes(slotQuotes)
                .orderLevelExtras(Collections.emptyList()) // 教练预约暂无订单级额外费用
                .sourcePlatform(1) // home平台
                .sellerId(dto.getCoachUserId())
                .bookingDate(dto.getBookingDate())
                .coachName(coachUserInfo.getNickName())
                .totalPrice(totalPrice)
                .serviceTypeDesc(serviceTypeDesc)
                .acceptableAreas(acceptableAreas)
                .venueRequirementDesc(firstTemplate.getVenueRequirementDesc())
                .build();
        return RpcResult.ok(resultDto);
    }

    /**
     * 验证服务类型兼容性
     * 检查所有时段模板是否支持用户选择的服务类型
     */
    private void validateServiceTypeCompatibility(
            List<CoachSlotTemplate> templates,
            Long serviceTypeId,
            Long coachUserId) {

        // 查询课程服务
        CoachCourseType courseType = courseTypeMapper.selectById(serviceTypeId);
        if (courseType == null || !courseType.getCoachUserId().equals(coachUserId)) {
            RpcResult.error(SERVICE_TYPE_UNKNOWN);
        }

        Integer courseServiceType = courseType.getCoachServiceTypeEnum();

        // 检查每个时段是否支持该服务类型
        for (CoachSlotTemplate template : templates) {
            if (!isServiceTypeCompatible(template.getCoachServiceType(), courseServiceType)) {
                    RpcResult.error(SERVICE_TYPE_NOT_SUPPORT);
            }
        }
    }

    /**
     * 检查服务类型兼容性
     *
     * @param templateServiceType 模板支持的服务类型（可能是组合）
     * @param courseServiceType   具体的课程服务类型
     * @return 是否兼容
     */
    private boolean isServiceTypeCompatible(Integer templateServiceType, Integer courseServiceType) {
        if (templateServiceType == null || courseServiceType == null) {
            return false;
        }

        // 0 表示均可以
        if (templateServiceType == 0) {
            return true;
        }

        // 完全匹配
        if (templateServiceType.equals(courseServiceType)) {
            return true;
        }

        // 检查组合类型
        // 5=1+2, 6=1+3, 7=1+4, 8=2+3, 9=2+4, 10=3+4
        // 11=1+2+3, 12=1+2+4, 13=1+3+4, 14=2+3+4
        Map<Integer, List<Integer>> compositeTypes = Map.of(
                5, Arrays.asList(1, 2),
                6, Arrays.asList(1, 3),
                7, Arrays.asList(1, 4),
                8, Arrays.asList(2, 3),
                9, Arrays.asList(2, 4),
                10, Arrays.asList(3, 4),
                11, Arrays.asList(1, 2, 3),
                12, Arrays.asList(1, 2, 4),
                13, Arrays.asList(1, 3, 4),
                14, Arrays.asList(2, 3, 4)
        );

        List<Integer> supportedTypes = compositeTypes.get(templateServiceType);
        return supportedTypes != null && supportedTypes.contains(courseServiceType);
    }

    /**
     * 获取服务类型名称（用于错误提示）
     */
    private String getServiceTypeName(Integer serviceType) {
        if (serviceType == null) {
            return "未知";
        }

        try {
            if (serviceType == 0) {
                return "全部类型";
            }

            // 组合类型的特殊处理
            Map<Integer, String> compositeNames = Map.of(
                    5, "一对一教学/一对一陪练",
                    6, "一对一教学/一对二",
                    7, "一对一教学/小班",
                    8, "一对一陪练/一对二",
                    9, "一对一陪练/小班",
                    10, "一对二/小班",
                    11, "一对一教学/一对一陪练/一对二",
                    12, "一对一教学/一对一陪练/小班",
                    13, "一对一教学/一对二/小班",
                    14, "一对一陪练/一对二/小班"
            );

            String compositeName = compositeNames.get(serviceType);
            if (compositeName != null) {
                return compositeName;
            }

            // 单一类型
            CoachServiceTypeEnum typeEnum = CoachServiceTypeEnum.fromValue(serviceType);
            return typeEnum.getDescription();
        } catch (Exception e) {
            return "未知类型(" + serviceType + ")";
        }
    }

    /**
     * 按需创建并锁定时段记录
     */
    private CoachSlotRecord lockSlotByTemplate(
            CoachSlotTemplate template,
            LocalDate bookingDate,
            Long userId) {

        // 检查是否已存在记录
        CoachSlotRecord existing = slotRecordMapper.selectOne(
                new LambdaQueryWrapper<CoachSlotRecord>()
                        .eq(CoachSlotRecord::getCoachSlotTemplateId, template.getCoachSlotTemplateId())
                        .eq(CoachSlotRecord::getBookingDate, bookingDate)
        );

        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(15);

        if (existing != null) {
            // 已有记录，尝试锁定
            int updated = slotRecordMapper.updateLockIfAvailable(
                    existing.getCoachSlotRecordId(),
                    1, // LOCKED
                    userId,
                    lockedUntil
            );

            if (updated == 0) {
                log.warn("时段记录已被锁定 - recordId: {}, userId: {},时段 {} -{} ",
                        existing.getCoachSlotRecordId(), userId , template.getStartTime(), template.getEndTime());
                RpcResult.error(SERVICE_TYPE_NOT_SUPPORT);
            }

            // 重新查询获取最新数据
            return slotRecordMapper.selectById(existing.getCoachSlotRecordId());
        }

        // 创建新记录并锁定
        CoachSlotRecord newRecord = new CoachSlotRecord();
        newRecord.setCoachSlotTemplateId(template.getCoachSlotTemplateId());
        newRecord.setCoachUserId(template.getCoachUserId());
        newRecord.setBookingDate(bookingDate);
        newRecord.setStartTime(template.getStartTime());
        newRecord.setEndTime(template.getEndTime());
        newRecord.setStatus(1); // LOCKED
        newRecord.setLockedByUserId(userId);
        newRecord.setLockedUntil(lockedUntil);
        newRecord.setLockedType(1); // 用户下单锁定
        newRecord.setOperatorId(userId);
        newRecord.setOperatorSource(2); // 用户端

        slotRecordMapper.insert(newRecord);
        log.info("创建新记录并锁定 - templateId: {}, date: {}, recordId: {}",
                template.getCoachSlotTemplateId(), bookingDate, newRecord.getCoachSlotRecordId());

        return newRecord;
    }

    @Override
    public RpcResult<CoachSnapshotResultDto> getCoachSnapshot(CoachSnapshotRequestDto dto) {
        log.info("获取教练基本信息快照 - coachUserId: {}", dto.getCoachUserId());

        // 1. 查询教练档案信息 (从数据库获取专业背景等)
        CoachProfile coachProfile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );
        if (coachProfile == null) {
            return RpcResult.error(COACH_INFO_NOT_EXIST);
        }

        // 2. 查询教练用户基础信息 (从用户中心/Dubbo获取头像、昵称等)
        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(dto.getCoachUserId());
        Assert.rpcResultOk(rpcResult);
        UserInfoVo coachUserInfo = rpcResult.getData();


        // 3. 构建并返回结果 (不包含时段信息)
        CoachSnapshotResultDto resultDto = CoachSnapshotResultDto.builder()
                .coachUserId(dto.getCoachUserId())
                .coachName(coachUserInfo.getNickName())
                .coachAvatar(coachUserInfo.getAvatarUrl())
                .coachPhone(null) // 隐私保护，通常不直接返回手机号
                .serviceArea(coachProfile.getCoachServiceArea())
                .certificationLevels(coachProfile.getCoachCertificationLevel())
                .teachingYears(coachProfile.getCoachTeachingYears())
                .specialtyTags(coachProfile.getCoachSpecialtyTags())
                .ratingScore(coachProfile.getCoachRatingScore())
                .ratingCount(coachProfile.getCoachRatingCount())
                .build();

        return RpcResult.ok(resultDto);
    }

    // ========== 辅助方法 ==========

    /**
     * 验证时段是否可用
     * 逻辑：查询是否存在不可用的记录
     */
    private void validateSlots(List<Long> slotTemplateIds, LocalDate bookingDate) {
        for (Long templateId : slotTemplateIds) {
            CoachSlotRecord record = slotRecordMapper.selectOne(
                    new LambdaQueryWrapper<CoachSlotRecord>()
                            .eq(CoachSlotRecord::getCoachSlotTemplateId, templateId)
                            .eq(CoachSlotRecord::getBookingDate, bookingDate)
            );

            if (record != null) {
                // 有记录，检查状态
                // 状态1=LOCKED, 2=UNAVAILABLE, 3=CUSTOM_EVENT
                if (record.getStatus() != 1) { // 不是AVAILABLE状态
                    CoachSlotTemplate template = slotTemplateMapper.selectById(templateId);
                    log.warn("时段不可用 - templateId: {}, date: {}, startTime: {}, endTime: {}, status: {}",
                            templateId, bookingDate, template.getStartTime(), template.getEndTime(), record.getStatus());
                    RpcResult.error(SLOT_UNAVAILABLE);
                }
                // 无记录 = 可用，继续
            }
        }
    }

    /**
     * 解析可接受区域
     */
    private List<String> parseAcceptableAreas(String acceptableAreasJson) {
        if (acceptableAreasJson == null || acceptableAreasJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            String cleaned = acceptableAreasJson.trim()
                    .replaceAll("^\\[|]$", "")
                    .replaceAll("\"", "");

            if (cleaned.isEmpty()) {
                return Collections.emptyList();
            }

            return Arrays.stream(cleaned.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("解析可接受区域失败: {}", acceptableAreasJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 构建锁键（基于模板ID + 日期）
     */
    private String buildLockKey(Long templateId, LocalDate bookingDate) {
        return CoachCacheConstants.COACH_SLOT_LOCK_KEY_PREFIX +
                templateId +
                CoachCacheConstants.COACH_SLOT_LOCK_KEY_SEPARATOR +
                bookingDate;
    }
}