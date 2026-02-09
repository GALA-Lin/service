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
import com.unlimited.sports.globox.model.coach.enums.CoachSlotRecordStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.common.result.CoachErrorCodeEnum.*;
import static com.unlimited.sports.globox.model.merchant.enums.OperatorSourceEnum.USER;


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
    public RpcResult<CoachPricingResultDto> quoteCoach(CoachPricingRequestDto dto) {
        log.info("教练预约价格查询 - userId: {}, coachUserId: {}, slotIds: {}, serviceTypeId: {}",
                dto.getUserId(), dto.getCoachUserId(), dto.getSlotIds(), dto.getServiceTypeId());

        // 参数验证
        if (dto.getSlotIds() == null || dto.getSlotIds().isEmpty()) {
            return RpcResult.error(PARAM_SLOT_EMPTY);
        }

        // 验证教练不能预约自己的课程
        if (dto.getUserId().equals(dto.getCoachUserId())) {
            return RpcResult.error(COACH_CANNOT_BOOK_SELF);
        }

        // 查询时段模板
        List<CoachSlotTemplate> templates = slotTemplateMapper.selectBatchIds(dto.getSlotIds());
        if (templates.size() != dto.getSlotIds().size()) {
            return RpcResult.error(SLOT_NOT_EXIST);
        }

        // 验证所有时段属于同一教练
        boolean allSameCoach = templates.stream()
                .allMatch(template -> template.getCoachUserId().equals(dto.getCoachUserId()));
        if (!allSameCoach) {
            return RpcResult.error(SLOT_COACH_NOT_MATCH);
        }

        // 查询教练档案信息（用于最低课时验证）
        CoachProfile coachProfile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );
        if (coachProfile == null) {
            return RpcResult.error(COACH_INFO_NOT_EXIST);
        }

        // 按时间排序模板
        List<CoachSlotTemplate> sortedTemplates = templates.stream()
                .sorted(Comparator.comparing(CoachSlotTemplate::getStartTime))
                .toList();

        // 验证时间连续性
        if(!validateTimeContinuity(sortedTemplates)) {
            return RpcResult.error(SLOT_NOT_CONTINUOUS);
        }

        // 计算总时长（小时）
        BigDecimal totalHours = calculateTotalHours(sortedTemplates);

        // 验证最低课时要求
        if(!validateMinimumHours(coachProfile, totalHours, dto)) {
            return RpcResult.error(MIN_HOURS_NOT_MET);
        }

        // 验证服务类型兼容性
        if(!validateServiceTypeCompatibility(templates, dto.getServiceTypeId(), dto.getCoachUserId())) {
            return RpcResult.error(SERVICE_TYPE_NOT_SUPPORT);
        }

        // 第一次验证（获取锁前）- 快速失败
        if(!validateSlots(dto.getSlotIds(), dto.getBookingDate())) {
            return RpcResult.error(SLOT_UNAVAILABLE);
        }

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
                return RpcResult.error(LOCK_ACQUIRE_FAILED);
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
     * 计算总时长（小时）
     */
    private BigDecimal calculateTotalHours(List<CoachSlotTemplate> templates) {
        int totalMinutes = templates.stream()
                .mapToInt(CoachSlotTemplate::getDurationMinutes)
                .sum();

        // 转换为小时，保留2位小数
        return BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /**
     * 验证最低课时要求
     *
     * @param coachProfile 教练档案
     * @param totalHours 总课时（小时）
     * @param dto 请求参数（用于获取预约区域信息）
     */
    private Boolean validateMinimumHours(
            CoachProfile coachProfile,
            BigDecimal totalHours,
            CoachPricingRequestDto dto) {

        // 获取教练的常驻服务区域和远距离服务区域
        String serviceAreas = coachProfile.getCoachServiceArea();
        List<String> remoteServiceAreas = coachProfile.getCoachRemoteServiceArea();

        // 从第一个时段模板获取可接受区域（所有时段的区域要求一致）
        CoachSlotTemplate firstTemplate = slotTemplateMapper.selectById(dto.getSlotIds().get(0));
        List<String> acceptableAreas = parseAcceptableAreas(firstTemplate.getAcceptableAreas());

        // 判断预约区域是否在远距离服务区域内
        boolean isRemoteArea = false;
        for (String area : acceptableAreas) {
            if (remoteServiceAreas.contains(area) && !serviceAreas.contains(area)) {
                isRemoteArea = true;
                break;
            }
        }

        // 获取最低课时要求
        int minHours = coachProfile.getCoachMinHours() != null ?
                coachProfile.getCoachMinHours() : 0; // 默认0小时
        int remoteMinHours = coachProfile.getCoachRemoteMinHours() != null ?
                coachProfile.getCoachRemoteMinHours() : 2; // 默认2小时

        if (isRemoteArea) {
            // 远距离区域：需要同时满足常驻区域最低课时和远距离最低课时
            BigDecimal requiredHours = BigDecimal.valueOf(Math.max(minHours, remoteMinHours));

            if (totalHours.compareTo(requiredHours) < 0) {
                log.warn("远距离区域课时不足 - 总课时: {}, 要求: {}", totalHours, requiredHours);
                RpcResult.error(REMOTE_AREA_MIN_HOURS_NOT_MET);
                //TODO 优化提示信息
                // String.format("远距离区域最低需预约%.1f小时", requiredHours)
                return false;
            }
        } else {
            // 常驻区域：只需满足常驻区域最低课时
            BigDecimal requiredHours = BigDecimal.valueOf(minHours);

            if (totalHours.compareTo(requiredHours) < 0) {
                log.warn("课时不足 - 总课时: {}, 最低要求: {}", totalHours, requiredHours);
                RpcResult.error(MIN_HOURS_NOT_MET);
                return false;
                //TODO 优化提示信息
//                String.format("最低需预约%.1f小时", requiredHours)
            }
        }

        log.info("课时验证通过 - 总课时: {}, 是否远距离: {}, 最低要求: {}",
                totalHours, isRemoteArea, isRemoteArea ? remoteMinHours : minHours);
        return true;
    }
    /**
     * 验证时间连续性
     */
    private Boolean validateTimeContinuity(List<CoachSlotTemplate> sortedTemplates) {
        if (sortedTemplates.size() <= 1) {
            return true; // 单个时段无需验证连续性
        }

        for (int i = 0; i < sortedTemplates.size() - 1; i++) {
            CoachSlotTemplate current = sortedTemplates.get(i);
            CoachSlotTemplate next = sortedTemplates.get(i + 1);

            // 验证当前时段的结束时间是否等于下一个时段的开始时间
            if (!current.getEndTime().equals(next.getStartTime())) {
                log.warn("时段不连续 - current: {}~{}, next: {}~{}",
                        current.getStartTime(), current.getEndTime(),
                        next.getStartTime(), next.getEndTime());
                return false;
            }
        }
        return true;
    }


    /**
     * 在事务中执行预约锁定和计价
     * 关键优化：
     * 1. 使用READ_COMMITTED隔离级别，防止幻读
     * 2. 使用原子性的CAS更新，防止超售
     * 3. 事务内二次验证
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public RpcResult<CoachPricingResultDto> executeBookingInTransaction(
            CoachPricingRequestDto dto,
            List<CoachSlotTemplate> templates) {

        // 第二次验证（事务内）- 确保数据一致性
        if (!validateSlots(dto.getSlotIds(), dto.getBookingDate())) {
            log.warn("【事务内】时段已被占用 - userId: {}, slotIds: {}", dto.getUserId(), dto.getSlotIds());
            return RpcResult.error(SLOT_UNAVAILABLE);
        }

        // 查询教练信息
        CoachProfile coachProfile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );
        if (coachProfile == null) {
            return RpcResult.error(COACH_INFO_NOT_EXIST);
        }

        // 查询用户信息
        RpcResult<UserInfoVo> rpcResult = userDubboService.getUserInfo(dto.getCoachUserId());
        Assert.rpcResultOk(rpcResult);
        UserInfoVo coachUserInfo = rpcResult.getData();

        // 查询用户选择的课程服务
        CoachCourseType courseType = courseTypeMapper.selectById(dto.getServiceTypeId());
        if (courseType == null || !courseType.getCoachUserId().equals(dto.getCoachUserId())) {
            return RpcResult.error(SERVICE_TYPE_UNKNOWN);
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

        // 按时间排序模板
        List<CoachSlotTemplate> sortedTemplates = templates.stream()
                .sorted(Comparator.comparing(CoachSlotTemplate::getStartTime))
                .toList();

        // 【关键改进】使用原子性锁定，防止超售
        List<CoachSlotRecord> lockedRecords = lockSlotsByTemplates(
                sortedTemplates,
                dto.getBookingDate(),
                dto.getUserId()
        );

        if (lockedRecords == null || lockedRecords.isEmpty()) {
            log.warn("【事务内】锁定时段失败 - 时段已被其他用户占用 - userId: {}", dto.getUserId());
            return RpcResult.error(SLOT_UNAVAILABLE);
        }

        log.info("【事务内】成功锁定{}个时段 - userId: {}", lockedRecords.size(), dto.getUserId());

        // 构建时段报价列表
        List<CoachSlotQuote> slotQuotes = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (int i = 0; i < sortedTemplates.size(); i++) {
            CoachSlotTemplate template = sortedTemplates.get(i);
            CoachSlotRecord record = lockedRecords.get(i);

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
                    .recordExtras(Collections.emptyList())
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
                .orderLevelExtras(Collections.emptyList())
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
     * 原子性锁定时段记录
     * 关键改进：
     * 1. 批量查询已存在的记录
     * 2. 使用CAS更新，确保只有AVAILABLE状态才能更新
     * 3. 保持模板和记录的顺序对应关系
     *
     * @param templates 时段模板列表
     * @param bookingDate 预订日期
     * @param userId 用户ID
     * @return 锁定的记录列表，如果锁定失败返回null
     */
    private List<CoachSlotRecord> lockSlotsByTemplates(
            List<CoachSlotTemplate> templates,
            LocalDate bookingDate,
            Long userId) {

        log.info("【事务内】开始原子性锁定时段 - userId: {}, 时段数: {}", userId, templates.size());

        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        // 提取所有模板ID
        List<Long> templateIds = templates.stream()
                .map(CoachSlotTemplate::getCoachSlotTemplateId)
                .collect(Collectors.toList());

        // 批量查询已存在的记录
        List<CoachSlotRecord> existingRecords = slotRecordMapper.selectList(
                new LambdaQueryWrapper<CoachSlotRecord>()
                        .in(CoachSlotRecord::getCoachSlotTemplateId, templateIds)
                        .eq(CoachSlotRecord::getBookingDate, bookingDate)
        );

        // 构建已有记录的模板ID集合
        Set<Long> existingTemplateIds = existingRecords.stream()
                .map(CoachSlotRecord::getCoachSlotTemplateId)
                .collect(Collectors.toSet());

        // 保持template和record的对应关系（使用LinkedHashMap保持顺序）
        Map<Long, CoachSlotRecord> templateToRecordMap = new LinkedHashMap<>();

        // 分组处理 - 区分要新增和要更新的记录
        Map<Boolean, List<CoachSlotTemplate>> partitioned = templates.stream()
                .collect(Collectors.partitioningBy(
                        t -> existingTemplateIds.contains(t.getCoachSlotTemplateId())));

        List<CoachSlotTemplate> toInsertTemplates = partitioned.get(false);
        List<CoachSlotTemplate> toUpdateTemplates = partitioned.get(true);

        LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(15);

        // 处理要新增的记录
        List<CoachSlotRecord> toInsert = toInsertTemplates.stream()
                .map(template -> {
                    CoachSlotRecord record = new CoachSlotRecord();
                    record.setCoachSlotTemplateId(template.getCoachSlotTemplateId());
                    record.setCoachUserId(template.getCoachUserId());
                    record.setBookingDate(bookingDate);
                    record.setStartTime(template.getStartTime());
                    record.setEndTime(template.getEndTime());
                    record.setStatus(1); // LOCKED
                    record.setLockedByUserId(userId);
                    record.setLockedUntil(lockedUntil);
                    record.setLockedType(1); // 用户下单锁定
                    record.setOperatorId(userId);
                    record.setOperatorSource(USER.getCode()); // 用户端
                    templateToRecordMap.put(template.getCoachSlotTemplateId(), record);
                    return record;
                })
                .collect(Collectors.toList());

        // 批量插入新记录
        if (!toInsert.isEmpty()) {
            coachSlotService.saveBatch(toInsert);
            log.info("【事务内】批量插入{}条新记录", toInsert.size());
        }

        // 处理要更新的记录 - 使用CAS更新
        List<Map.Entry<CoachSlotRecord, Integer>> updateResults = new ArrayList<>();

        for (CoachSlotTemplate template : toUpdateTemplates) {
            // 查找对应的已存在记录
            CoachSlotRecord existingRecord = existingRecords.stream()
                    .filter(r -> r.getCoachSlotTemplateId().equals(template.getCoachSlotTemplateId()))
                    .findFirst()
                    .orElse(null);

            if (existingRecord != null) {
                // 使用原子性的CAS更新（只有status=0即AVAILABLE时才能更新）
                int updated = slotRecordMapper.updateLockIfAvailable(
                        existingRecord.getCoachSlotRecordId(),
                        CoachSlotRecordStatusEnum.LOCKED.getCode(), // LOCKED
                        userId,
                        lockedUntil
                );

                updateResults.add(new AbstractMap.SimpleEntry<>(existingRecord, updated));

                if (updated > 0) {
                    // 更新成功，重新查询获取最新数据
                    CoachSlotRecord updatedRecord = slotRecordMapper.selectById(
                            existingRecord.getCoachSlotRecordId());
                    templateToRecordMap.put(template.getCoachSlotTemplateId(), updatedRecord);
                }
            }
        }

        // 检查是否有更新失败的记录（超售防护）
        Optional<Map.Entry<CoachSlotRecord, Integer>> failedUpdate = updateResults.stream()
                .filter(entry -> entry.getValue() == 0)
                .findFirst();

        if (failedUpdate.isPresent()) {
            CoachSlotRecord failedRecord = failedUpdate.get().getKey();
            log.error("【事务内】时段已被其他用户占用，CAS更新失败 - recordId: {}, templateId: {}",
                    failedRecord.getCoachSlotRecordId(), failedRecord.getCoachSlotTemplateId());
            return null; // 返回null表示锁定失败
        }

        int successUpdateCount = (int) updateResults.stream()
                .filter(entry -> entry.getValue() > 0)
                .count();

        // 按照templates的顺序返回records，确保顺序正确
        List<CoachSlotRecord> result = templates.stream()
                .map(template -> templateToRecordMap.get(template.getCoachSlotTemplateId()))
                .collect(Collectors.toList());

        log.info("【事务内】时段锁定完成 - userId: {}, 总数: {}, 新增: {}, 更新: {} (成功: {})",
                userId, templates.size(), toInsert.size(), toUpdateTemplates.size(), successUpdateCount);

        return result;
    }

    /**
     * 验证服务类型兼容性
     * 检查所有时段模板是否支持用户选择的服务类型
     */
    private Boolean validateServiceTypeCompatibility(
            List<CoachSlotTemplate> templates,
            Long serviceTypeId,
            Long coachUserId) {

        // 查询课程服务
        CoachCourseType courseType = courseTypeMapper.selectById(serviceTypeId);
        if (courseType == null || !courseType.getCoachUserId().equals(coachUserId)) {
            return false;
        }

        Integer courseServiceType = courseType.getCoachServiceTypeEnum();

        // 检查每个时段是否支持该服务类型
        for (CoachSlotTemplate template : templates) {
            if (!isServiceTypeCompatible(template.getCoachServiceType(), courseServiceType)) {
                    return false;
            }
        }
        return true;
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

        String venue = null;
        String remark = null;
        if (dto.getRecordList() != null && !dto.getRecordList().isEmpty()) {
            List<CoachSlotRecord> slotRecords = slotRecordMapper.selectBatchIds(dto.getRecordList());
            // 只要有一个 Record venue / remark有值就返回
            if (!slotRecords.isEmpty()) {
                // 1. 寻找第一个不为空的场馆名 (使用 Stream 过滤 null 和 空字符串)
                venue = slotRecords.stream()
                        .map(CoachSlotRecord::getVenue)
                        .filter(v -> v != null && !v.trim().isEmpty())
                        .findFirst()
                        .orElse(null);

                // 2. 寻找第一个不为空的备注
                remark = slotRecords.stream()
                        .map(CoachSlotRecord::getRemark)
                        .filter(r -> r != null && !r.trim().isEmpty())
                        .findFirst()
                        .orElse(null);
            }
        }

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
                .venue(venue)
                .remark(remark)
                .build();

        return RpcResult.ok(resultDto);
    }

    // ========== 辅助方法 ==========

    /**
     * 验证时段是否可用（只读验证）
     * 逻辑：查询是否存在不可用的记录
     */
    private Boolean validateSlots(List<Long> slotTemplateIds, LocalDate bookingDate) {
        if (slotTemplateIds == null || slotTemplateIds.isEmpty()) {
            return true;
        }

        for (Long templateId : slotTemplateIds) {
            CoachSlotRecord record = slotRecordMapper.selectOne(
                    new LambdaQueryWrapper<CoachSlotRecord>()
                            .eq(CoachSlotRecord::getCoachSlotTemplateId, templateId)
                            .eq(CoachSlotRecord::getBookingDate, bookingDate)
            );

            if (record != null) {
                // 有记录，检查状态
                // 状态：0=AVAILABLE, 1=LOCKED, 2=UNAVAILABLE, 3=CUSTOM_EVENT
                if (!record.getStatus().equals(CoachSlotRecordStatusEnum.AVAILABLE.getCode())) { // 不是AVAILABLE状态
                    CoachSlotTemplate template = slotTemplateMapper.selectById(templateId);
                    log.warn("时段不可用 - templateId: {}, date: {}, startTime: {}, endTime: {}, status: {}",
                            templateId, bookingDate,
                            template != null ? template.getStartTime() : "unknown",
                            template != null ? template.getEndTime() : "unknown",
                            record.getStatus());
                    return false;
                }
            }
            // 无记录 = 可用，继续
        }
        return true;
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