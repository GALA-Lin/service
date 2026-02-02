package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.coach.mapper.*;
import com.unlimited.sports.globox.coach.service.ICoachSlotService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.UserPhoneDto;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.entity.*;
import com.unlimited.sports.globox.model.coach.enums.CoachServiceTypeEnum;
import com.unlimited.sports.globox.model.coach.enums.CoachSlotLockType;
import com.unlimited.sports.globox.model.coach.enums.CoachSlotRecordStatusEnum;
import com.unlimited.sports.globox.model.coach.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教练时段管理服务实现
 * 核心设计:按需生成记录,节约存储空间
 * - 无记录 = 可用
 * - 有记录且状态为AVAILABLE(1) = 可用
 * - 有记录且状态为LOCKED(2)/UNAVAILABLE(3)/CUSTOM_EVENT(4) = 不可用
 */
@Slf4j
@Service
public class CoachSlotServiceImpl extends ServiceImpl<CoachSlotRecordMapper, CoachSlotRecord>
        implements ICoachSlotService {

    @Autowired
    private CoachSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private CoachSlotRecordMapper slotRecordMapper;

    @Autowired
    private CoachCustomScheduleMapper customScheduleMapper;

    @Autowired
    private CoachBookingsMapper bookingsMapper;

    @Autowired
    private CoachSlotBatchOperationLogMapper batchLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 1. 注入用户服务
    @DubboReference(group = "rpc", timeout = 10000)
    private UserDubboService userDubboService;

    /**
     * 初始化时段模板(不生成记录)
     *
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer initSlotTemplates(CoachSlotTemplateInitDto dto) {
        log.info("初始化时段模板 - coachUserId: {}, 时段数: {}",
                dto.getCoachUserId(), dto.getSlots().size());

        // 验证时段是否冲突
        for (CoachSlotTemplateInitDto.SlotTemplateItem item : dto.getSlots()) {
            int conflicts = slotTemplateMapper.countConflicts(
                    dto.getCoachUserId(),
                    item.getStartTime(),
                    item.getEndTime(),
                    0L
            );
            if (conflicts > 0) {
                throw new GloboxApplicationException(
                        String.format("时段 %s-%s 与已有模板冲突",
                                item.getStartTime(), item.getEndTime()));
            }
        }

        // 批量创建模板(不创建记录)
        List<CoachSlotTemplate> templates = dto.getSlots().stream()
                .map(item -> {
                    CoachSlotTemplate template = new CoachSlotTemplate();
                    template.setCoachUserId(dto.getCoachUserId());
                    template.setStartTime(item.getStartTime());
                    template.setEndTime(item.getEndTime());
                    template.setDurationMinutes(dto.getSlotDurationMinutes());
                    template.setCoachServiceType(item.getCoachServiceType());
                    template.setPrice(item.getPrice());
                    template.setAcceptableAreas(convertListToJson(item.getAcceptableAreas()));
                    template.setVenueRequirementDesc(item.getVenueRequirementDesc());
                    template.setAdvanceBookingDays(dto.getAdvanceBookingDays());
                    template.setIsDeleted(0);
                    return template;
                })
                .toList();

        templates.forEach(slotTemplateMapper::insert);

        log.info("时段模板初始化完成,共创建 {} 个模板(按需生成记录)", templates.size());
        return templates.size();
    }

    /**
     * 更新时段模板
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSlotTemplate(Long templateId, CoachSlotTemplateUpdateDto dto) {
        log.info("更新时段模板 - templateId: {}", templateId);

        CoachSlotTemplate template = slotTemplateMapper.selectById(templateId);
        if (template == null || !template.getCoachUserId().equals(dto.getCoachUserId())) {
            throw new GloboxApplicationException("时段模板不存在或无权限");
        }

        if (dto.getPrice() != null) {
            template.setPrice(dto.getPrice());
        }
        if (dto.getAcceptableAreas() != null) {
            template.setAcceptableAreas(convertListToJson(dto.getAcceptableAreas()));
        }
        if (dto.getVenueRequirementDes() != null) {
            template.setVenueRequirementDesc(dto.getVenueRequirementDes());
        }
        if (dto.getAdvanceBookingDays() != null) {
            template.setAdvanceBookingDays(dto.getAdvanceBookingDays());
        }

        slotTemplateMapper.updateById(template);
        log.info("时段模板更新成功");
    }

    /**
     * 删除时段模板
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSlotTemplate(Long templateId, Long coachUserId) {
        log.info("删除时段模板 - templateId: {}", templateId);

        CoachSlotTemplate template = slotTemplateMapper.selectById(templateId);
        if (template == null || !template.getCoachUserId().equals(coachUserId)) {
            throw new GloboxApplicationException("时段模板不存在或无权限");
        }

        // 软删除
        template.setIsDeleted(1);
        slotTemplateMapper.updateById(template);

        log.info("时段模板删除成功");
    }

    /**
     * 查询教练的所有时段模板
     */
    @Override
    public List<CoachSlotRecordVo> getSlotTemplates(Long coachUserId) {
        log.info("查询时段模板 - coachUserId: {}", coachUserId);

        List<CoachSlotTemplate> templates = slotTemplateMapper.selectList(
                new LambdaQueryWrapper<CoachSlotTemplate>()
                        .eq(CoachSlotTemplate::getCoachUserId, coachUserId)
                        .eq(CoachSlotTemplate::getIsDeleted, 0)
                        .orderByAsc(CoachSlotTemplate::getStartTime)
        );

        return templates.stream()
                .map(this::buildSlotRecordVo)
                .collect(Collectors.toList());
    }

    /**
     * 查询可预约时段
     * 核心逻辑修正：
     * - 无记录 = 可用（状态0）
     * - 有记录且status=0 = 可用
     * - 有记录且status=1/2/3 = 不可用
     */
    @Override
    public Map<String, List<CoachAvailableSlotVo>> getAvailableSlots(CoachAvailableSlotQueryDto dto) {
        log.info("查询可预约时段 - coachUserId: {}, 日期范围: {} - {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        // 1. 查询所有有效模板
        LambdaQueryWrapper<CoachSlotTemplate> templateWrapper = new LambdaQueryWrapper<CoachSlotTemplate>()
                .eq(CoachSlotTemplate::getCoachUserId, dto.getCoachUserId())
                .eq(CoachSlotTemplate::getIsDeleted, 0);

        if (dto.getCoachServiceType() != null) {
            templateWrapper.and(w -> {
                w.eq(CoachSlotTemplate::getCoachServiceType, 0)
                        .or().eq(CoachSlotTemplate::getCoachServiceType, dto.getCoachServiceType())
                        .or().isNull(CoachSlotTemplate::getCoachServiceType);
            });
        }

        List<CoachSlotTemplate> templates = slotTemplateMapper.selectList(templateWrapper);
        if (templates.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 批量查询日期范围内的所有记录
        List<CoachSlotRecord> allRecords = slotRecordMapper.selectByDateRange(
                dto.getCoachUserId(),
                dto.getStartDate(),
                dto.getEndDate()
        );

        // 3. 按日期和模板ID组织记录
        Map<String, Map<Long, CoachSlotRecord>> recordMap = buildRecordMapByDate(allRecords);

        // 4. 遍历日期和模板，计算可用时段
        Map<String, List<CoachAvailableSlotVo>> availableSlotsMap = new LinkedHashMap<>();

        LocalDate currentDate = dto.getStartDate();
        while (!currentDate.isAfter(dto.getEndDate())) {
            LocalDate finalCurrentDate = currentDate;
            String dateKey = currentDate.toString();

            List<CoachAvailableSlotVo> daySlots = new ArrayList<>();
            Map<Long, CoachSlotRecord> dateRecords = recordMap.getOrDefault(dateKey, Collections.emptyMap());

            for (CoachSlotTemplate template : templates) {
                // 检查是否在提前预约天数范围内
                long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), finalCurrentDate);
                if (daysUntil < 0 || daysUntil > template.getAdvanceBookingDays()) {
                    continue;
                }

                CoachSlotRecord record = dateRecords.get(template.getCoachSlotTemplateId());

                // 构建可用时段Vo对象，并设置正确的状态
                CoachAvailableSlotVo slotVo = buildAvailableSlotVo(template, finalCurrentDate, record);

                // 【关键修改】状态判断逻辑
                if (record == null) {
                    // 无记录 = 可用
                    slotVo.setSlotStatus(CoachSlotRecordStatusEnum.AVAILABLE.getCode());
                    slotVo.setSlotStatusDesc(CoachSlotRecordStatusEnum.AVAILABLE.getDescription());
                } else {
                    // 有记录，使用记录的实际状态
                    slotVo.setSlotStatus(record.getStatus());
                    slotVo.setSlotStatusDesc(CoachSlotRecordStatusEnum.getDescription(record.getStatus()));
                }

                daySlots.add(slotVo);
            }

            if (!daySlots.isEmpty()) {
                availableSlotsMap.put(dateKey, daySlots);
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("找到可预约日期数: {}, 总时段数: {}",
                availableSlotsMap.size(),
                availableSlotsMap.values().stream().mapToInt(List::size).sum());
        return availableSlotsMap;
    }

    /**
     * 查询时段可用性状态
     * 修正：统一状态判断逻辑
     */
    @Override
    public Map<String, Boolean> checkSlotAvailability(CoachAvailableSlotQueryDto dto) {
        log.info("检查时段可用性 - coachUserId: {}", dto.getCoachUserId());

        // 查询模板
        LambdaQueryWrapper<CoachSlotTemplate> templateWrapper = new LambdaQueryWrapper<CoachSlotTemplate>()
                .eq(CoachSlotTemplate::getCoachUserId, dto.getCoachUserId())
                .eq(CoachSlotTemplate::getIsDeleted, 0);

        if (dto.getCoachServiceType() != null) {
            templateWrapper.and(w -> w.eq(CoachSlotTemplate::getCoachServiceType, dto.getCoachServiceType())
                    .or().isNull(CoachSlotTemplate::getCoachServiceType));
        }

        List<CoachSlotTemplate> templates = slotTemplateMapper.selectList(templateWrapper);
        if (templates.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查询记录
        List<CoachSlotRecord> records = slotRecordMapper.selectByDateRange(
                dto.getCoachUserId(),
                dto.getStartDate(),
                dto.getEndDate()
        );

        Map<String, Map<Long, CoachSlotRecord>> recordMap = buildRecordMapByDate(records);
        Map<String, Boolean> availabilityMap = new HashMap<>();

        LocalDate currentDate = dto.getStartDate();
        while (!currentDate.isAfter(dto.getEndDate())) {
            String dateKey = currentDate.toString();
            Map<Long, CoachSlotRecord> dateRecords = recordMap.getOrDefault(dateKey, Collections.emptyMap());

            for (CoachSlotTemplate template : templates) {
                long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), currentDate);
                if (daysUntil < 0 || daysUntil > template.getAdvanceBookingDays()) {
                    continue;
                }

                CoachSlotRecord record = dateRecords.get(template.getCoachSlotTemplateId());
                String key = String.format("%s_%d_%s-%s",
                        dateKey,
                        template.getCoachSlotTemplateId(),
                        template.getStartTime(),
                        template.getEndTime());

                // 【关键修改】可用性判断: 无记录或记录状态为0(AVAILABLE)
                boolean isAvailable = (record == null) ||
                        (record.getStatus().equals(CoachSlotRecordStatusEnum.AVAILABLE.getCode()));
                availabilityMap.put(key, isAvailable);
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("可用性检查完成 - 总时段数: {}, 可用数: {}",
                availabilityMap.size(),
                availabilityMap.values().stream().filter(Boolean::booleanValue).count());

        return availabilityMap;
    }


    /**
     * 查询教练日程
     * 修正：状态过滤条件，并返回 lockedByUserId
     */
    @Override
    public List<CoachScheduleVo> getCoachSchedule(CoachScheduleQueryDto dto) {
        log.info("查询教练日程 - coachUserId: {}, {} 至 {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        // 【关键修改】查询锁定的时段记录
        // 状态1=LOCKED(用户下单锁定) 且 锁定类型1=用户下单锁定
        List<CoachSlotRecord> slotRecords = slotRecordMapper.selectList(
                new LambdaQueryWrapper<CoachSlotRecord>()
                        .eq(CoachSlotRecord::getCoachUserId, dto.getCoachUserId())
                        .between(CoachSlotRecord::getBookingDate, dto.getStartDate(), dto.getEndDate())
                        .eq(CoachSlotRecord::getStatus, CoachSlotRecordStatusEnum.LOCKED.getCode()) // 状态=锁定
                        .eq(CoachSlotRecord::getLockedType, CoachSlotLockType.USER_ORDER_LOCK.getCode()) // 用户下单锁定
        );

        List<Long> studentIds = slotRecords.stream()
                .map(CoachSlotRecord::getLockedByUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserInfoVo> userMap = new HashMap<>();
        Map<Long, String> phoneMap = new HashMap<>();

        if (!studentIds.isEmpty()) {
            // 批量获取昵称
            BatchUserInfoRequest userReq = new BatchUserInfoRequest();
            userReq.setUserIds(studentIds);
            userMap = userDubboService.batchGetUserInfo(userReq).getData().getUsers().stream()
                    .collect(Collectors.toMap(UserInfoVo::getUserId, u -> u));

            // 批量获取明文手机号
            phoneMap = userDubboService.batchGetUserPhone(studentIds).getData().stream()
                    .collect(Collectors.toMap(UserPhoneDto::getUserId, UserPhoneDto::getPhone));
        }

        Map<Long, UserInfoVo> finalUserMap = userMap;
        Map<Long, String> finalPhoneMap = phoneMap;

        // 【关键修改】构建平台日程时添加 lockedByUserId
        List<CoachScheduleVo> schedules = slotRecords.stream()
                .map(record -> {
                    UserInfoVo user = finalUserMap.get(record.getLockedByUserId());
                    return CoachScheduleVo.builder()
                            .scheduleDate(record.getBookingDate())
                            .startTime(record.getStartTime())
                            .endTime(record.getEndTime())
                            .scheduleType("PLATFORM_SLOT")
                            .studentName(user != null ? user.getNickName() : "未知学员")
                            .studentPhone(finalPhoneMap.get(record.getLockedByUserId()))
                            .venue(record.getVenue())
                            .remark(record.getRemark())
                            .bookingId(record.getCoachSlotRecordId())
                            .lockedByUserId(record.getLockedByUserId()) // 新增：返回锁定用户ID
                            .build();
                })
                .collect(Collectors.toList());

        // 4. 处理自定义日程 (自定义日程通常由教练输入，直接从数据库字段映射)
        if (dto.getIncludeCustomSchedule()) {
            List<CoachCustomSchedule> customSchedules = customScheduleMapper.selectByDateRange(
                    dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

            schedules.addAll(customSchedules.stream()
                    .map(custom -> CoachScheduleVo.builder()
                            .scheduleDate(custom.getScheduleDate())
                            .startTime(custom.getStartTime())
                            .endTime(custom.getEndTime())
                            .scheduleType("CUSTOM_EVENT")
                            .studentName(custom.getStudentName())
                            .venue(custom.getVenueName())
                            .remark(custom.getRemark())
                            .customScheduleId(custom.getCoachCustomScheduleId())
                            .lockedByUserId(null) // 自定义日程没有 lockedByUserId
                            .build())
                    .toList());
        }

        // 5. 排序逻辑 (必须在所有数据收集完后执行)
        schedules.sort(Comparator.comparing(CoachScheduleVo::getScheduleDate)
                .thenComparing(CoachScheduleVo::getStartTime));

        return schedules;
    }

    /**
     * 锁定时段
     * 修正：状态码使用枚举
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockSlot(CoachSlotLockDto dto) {
        log.info("锁定时段 - slotRecordId: {}, userId: {}",
                dto.getSlotRecordId(), dto.getUserId());

        LocalDateTime lockedUntil = LocalDateTime.now()
                .plusMinutes(dto.getLockMinutes());

        // 如果提供了recordId，直接更新
        if (dto.getSlotRecordId() != null) {
            CoachSlotRecord record = slotRecordMapper.selectById(dto.getSlotRecordId());
            if (record == null) {
                throw new GloboxApplicationException("时段记录不存在");
            }
            int updated = slotRecordMapper.updateLockIfAvailable(
                    dto.getSlotRecordId(),
                    CoachSlotRecordStatusEnum.LOCKED.getCode(), // 使用枚举
                    dto.getUserId(),
                    lockedUntil
            );

            if (updated > 0) {
                if (dto.getVenue() != null || dto.getRemark() != null) {
                    record = slotRecordMapper.selectById(dto.getSlotRecordId());
                    record.setVenue(dto.getVenue());
                    record.setRemark(dto.getRemark());
                    slotRecordMapper.updateById(record);
                }
                log.info("时段锁定成功 - slotRecordId: {}", dto.getSlotRecordId());
                return true;
            } else {
                log.warn("时段锁定失败，可能已被占用 - slotRecordId: {}", dto.getSlotRecordId());
                return false;
            }
        }

        // 没有recordId，需要按需创建
        if (dto.getTemplateId() == null || dto.getBookingDate() == null) {
            throw new GloboxApplicationException("缺少必要参数:templateId或bookingDate");
        }

        // 检查是否已存在记录
        CoachSlotRecord existing = slotRecordMapper.selectByTemplateIdAndDate(
                dto.getTemplateId(), dto.getBookingDate());

        if (existing != null) {
            // 已有记录，尝试锁定
            return lockSlot(CoachSlotLockDto.builder()
                    .slotRecordId(existing.getCoachSlotRecordId())
                    .userId(dto.getUserId())
                    .lockMinutes(dto.getLockMinutes())
                    .build());
        }

        // 创建新记录并锁定
        CoachSlotTemplate template = slotTemplateMapper.selectById(dto.getTemplateId());
        if (template == null) {
            throw new GloboxApplicationException("时段模板不存在");
        }

        CoachSlotRecord record = new CoachSlotRecord();
        record.setCoachSlotTemplateId(dto.getTemplateId());
        record.setCoachUserId(template.getCoachUserId());
        record.setBookingDate(dto.getBookingDate());
        record.setStartTime(template.getStartTime());
        record.setEndTime(template.getEndTime());
        record.setStatus(CoachSlotRecordStatusEnum.LOCKED.getCode()); // 使用枚举
        record.setLockedByUserId(dto.getUserId());
        record.setLockedUntil(lockedUntil);
        record.setLockedType(CoachSlotLockType.USER_ORDER_LOCK.getCode()); // 使用枚举
        record.setOperatorId(dto.getUserId());
        record.setOperatorSource(2); // 用户端
        record.setVenue(dto.getVenue());
        record.setRemark(dto.getRemark());
        slotRecordMapper.insert(record);
        log.info("创建新记录并锁定成功 - recordId: {}", record.getCoachSlotRecordId());
        return true;
    }

    /**
     * 解锁时段
     * 修正：状态码使用枚举
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlockSlot(Long slotRecordId, Long userId) {
        log.info("解锁时段 - slotRecordId: {}, userId: {}", slotRecordId, userId);

        CoachSlotRecord record = slotRecordMapper.selectById(slotRecordId);
        if (record == null) {
            throw new GloboxApplicationException("时段记录不存在");
        }

        // 验证是否是当前用户锁定的
        if (!userId.equals(record.getLockedByUserId())) {
            throw new GloboxApplicationException("只能解锁自己锁定的时段");
        }


        record.setStatus(CoachSlotRecordStatusEnum.AVAILABLE.getCode());
        record.setLockedByUserId(null);
        record.setLockedUntil(null);
        record.setLockReason(null);
        slotRecordMapper.updateById(record);
        log.info("时段解锁成功 - slotRecordId: {}", slotRecordId);
        
    }

    /**
     * 批量锁定时段（教练端操作）
     * 修正：状态码使用枚举
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchLockSlots(CoachSlotBatchLockDto dto) {
        log.info("批量锁定时段 - coachUserId: {}, {} 至 {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        // 查询模板
        List<CoachSlotTemplate> templates = slotTemplateMapper.selectList(
                new LambdaQueryWrapper<CoachSlotTemplate>()
                        .eq(CoachSlotTemplate::getCoachUserId, dto.getCoachUserId())
                        .eq(CoachSlotTemplate::getIsDeleted, 0)
        );

        if (templates.isEmpty()) {
            return 0;
        }

        // 查询现有记录
        List<CoachSlotRecord> existingRecords = slotRecordMapper.selectByDateRange(
                dto.getCoachUserId(),
                dto.getStartDate(),
                dto.getEndDate()
        );

        Map<String, Map<Long, CoachSlotRecord>> recordMap = buildRecordMapByDate(existingRecords);

        int lockedCount = 0;
        LocalDate currentDate = dto.getStartDate();

        while (!currentDate.isAfter(dto.getEndDate())) {
            String dateKey = currentDate.toString();
            Map<Long, CoachSlotRecord> dateRecords = recordMap.getOrDefault(dateKey, new HashMap<>());

            for (CoachSlotTemplate template : templates) {
                // 时间范围过滤
                if (dto.getStartTime() != null && dto.getEndTime() != null) {
                    if (template.getStartTime().isBefore(dto.getStartTime()) ||
                            template.getEndTime().isAfter(dto.getEndTime())) {
                        continue;
                    }
                }

                CoachSlotRecord record = dateRecords.get(template.getCoachSlotTemplateId());

                if (record == null) {
                    // 创建新记录并锁定
                    record = new CoachSlotRecord();
                    record.setCoachSlotTemplateId(template.getCoachSlotTemplateId());
                    record.setCoachUserId(dto.getCoachUserId());
                    record.setBookingDate(currentDate);
                    record.setStartTime(template.getStartTime());
                    record.setEndTime(template.getEndTime());
                    record.setStatus(CoachSlotRecordStatusEnum.UNAVAILABLE.getCode()); // 不可预约
                    record.setLockedType(CoachSlotLockType.COACH_MANUAL_LOCK.getCode()); // 教练手动锁定
                    record.setLockReason(dto.getLockReason());
                    record.setOperatorId(dto.getOperatorId());
                    record.setOperatorSource(1); // 教练端
                    slotRecordMapper.insert(record);
                    lockedCount++;
                } else if (CoachSlotRecordStatusEnum.AVAILABLE.getCode().equals(record.getStatus())) {
                    // 更新可用记录为锁定
                    record.setStatus(CoachSlotRecordStatusEnum.UNAVAILABLE.getCode());
                    record.setLockedType(CoachSlotLockType.COACH_MANUAL_LOCK.getCode());
                    record.setLockReason(dto.getLockReason());
                    record.setOperatorId(dto.getOperatorId());
                    slotRecordMapper.updateById(record);
                    lockedCount++;
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        // 记录操作日志
        logBatchOperation(dto.getCoachUserId(), 2,
                dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(),
                lockedCount, dto.getLockReason(), dto.getOperatorId());

        log.info("批量锁定完成 - 成功锁定: {} 个时段", lockedCount);
        return lockedCount;
    }

    /**
     * 批量解锁时段（教练端操作）
     * 修正：状态判断逻辑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnlockSlots(CoachSlotBatchUnlockDto dto) {
        log.info("批量解锁时段 - coachUserId: {}, {} 至 {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        List<CoachSlotRecord> records = slotRecordMapper.selectByDateRange(
                dto.getCoachUserId(),
                dto.getStartDate(),
                dto.getEndDate()
        );

        // 过滤：只解锁教练手动锁定的（状态2=UNAVAILABLE 且 锁定类型2=教练手动锁定）
        List<CoachSlotRecord> lockedRecords = records.stream()
                .filter(r -> CoachSlotRecordStatusEnum.UNAVAILABLE.getCode().equals(r.getStatus()) &&
                        CoachSlotLockType.COACH_MANUAL_LOCK.getCode().equals(r.getLockedType()))
                .filter(r -> {
                    if (dto.getStartTime() != null && dto.getEndTime() != null) {
                        return !r.getStartTime().isBefore(dto.getStartTime()) &&
                                !r.getEndTime().isAfter(dto.getEndTime());
                    }
                    return true;
                })
                .toList();

        int unlockedCount = 0;
        for (CoachSlotRecord record : lockedRecords) {
            record.setStatus(CoachSlotRecordStatusEnum.AVAILABLE.getCode());
            record.setLockedByUserId(null);
            record.setLockedUntil(null);
            record.setLockReason(null);
            record.setLockedType(null);
            slotRecordMapper.updateById(record);
            unlockedCount++;
        }

        logBatchOperation(dto.getCoachUserId(), 3,
                dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(),
                unlockedCount, "批量解锁", dto.getOperatorId());

        log.info("批量解锁完成 - 成功解锁: {} 个时段", unlockedCount);
        return unlockedCount;
    }

    /**
     * 批量解锁时段（通过recordIds）
     * 修正：状态判断逻辑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnlockSlots(List<Long> recordIds, Long userId) {

        if (recordIds == null || recordIds.isEmpty()) {
            log.warn("recordIds为空，无需解锁");
            return 0;
        }

        log.info("批量解锁时段(通过recordIds) - userId: {}, recordIds数量: {}",
                userId, recordIds.size());

        int unlockedCount = 0;
        List<String> failedRecords = new ArrayList<>();

        for (Long recordId : recordIds) {
            try {
                CoachSlotRecord record = slotRecordMapper.selectById(recordId);

                if (record == null) {
                    log.warn("时段记录不存在 - recordId: {}", recordId);
                    failedRecords.add(recordId + "(不存在)");
                    continue;
                }

                // 验证是否是当前用户锁定的
                if (!userId.equals(record.getLockedByUserId())) {
                    log.warn("只能解锁自己锁定的时段 - recordId: {}, lockedBy: {}, currentUser: {}",
                            recordId, record.getLockedByUserId(), userId);
                    failedRecords.add(recordId + "(权限不足)");
                    continue;
                }

                // 检查时段状态（只能解锁LOCKED状态的记录）
                if (!CoachSlotRecordStatusEnum.LOCKED.getCode().equals(record.getStatus())) {
                    log.warn("时段不是锁定状态，无需解锁 - recordId: {}, status: {}",
                            recordId, record.getStatus());
                    failedRecords.add(recordId + "(状态异常:" + record.getStatus() + ")");
                    continue;
                }


                record.setStatus(CoachSlotRecordStatusEnum.AVAILABLE.getCode());
                record.setLockedByUserId(null);
                record.setLockedUntil(null);
                record.setLockReason(null);
                record.setLockedType(null);
                slotRecordMapper.updateById(record);
                log.info("更新锁定状态为可用 - recordId: {}", recordId);


                unlockedCount++;

            } catch (Exception e) {
                log.error("解锁单个时段失败 - recordId: {}", recordId, e);
                failedRecords.add(recordId + "(异常:" + e.getMessage() + ")");
            }
        }

        // 记录批量操作日志
        if (unlockedCount > 0) {
            log.info("批量解锁完成 - userId: {}, 成功: {}/{}, 失败记录: {}",
                    userId, unlockedCount, recordIds.size(),
                    failedRecords.isEmpty() ? "无" : String.join(", ", failedRecords));
        }

        if (!failedRecords.isEmpty()) {
            log.warn("部分时段解锁失败 - userId: {}, 失败详情: {}", userId, failedRecords);
        }

        return unlockedCount;
    }

    /**
     * 更新时段的场地和备注信息
     * 教练可以在确认订单后修改上课地点和备注
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateSlotVenue(UpdateCoachSlotVenueDto dto) {
        log.info("更新时段场地信息 - slotRecordId: {}, coachUserId: {}",
                dto.getSlotRecordId(), dto.getCoachUserId());

        CoachSlotRecord record = slotRecordMapper.selectById(dto.getSlotRecordId());
        if (record == null) {
            throw new GloboxApplicationException("时段记录不存在");
        }

        // 验证权限：只有该教练可以修改自己的时段
        if (!record.getCoachUserId().equals(dto.getCoachUserId())) {
            throw new GloboxApplicationException("无权限修改该时段");
        }

        // 更新场地和备注信息
        record.setVenue(dto.getVenue());
        record.setRemark(dto.getRemark());

        slotRecordMapper.updateById(record);
        log.info("时段场地信息更新成功 - slotRecordId: {}", dto.getSlotRecordId());
    }


    /**
     * 创建自定义日程(按需生成占位记录)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomSchedule(CoachCustomScheduleDto dto) {
        log.info("创建自定义日程 - coachUserId: {}, date: {}",
                dto.getCoachUserId(), dto.getScheduleDate());

        // 验证时间合法性
        validateTimeRange(dto.getStartTime(), dto.getEndTime());

        // 检查时间冲突
        List<CoachSlotConflictVo> conflicts = checkTimeConflicts(
                dto.getCoachUserId(),
                dto.getScheduleDate(),
                dto.getStartTime(),
                dto.getEndTime());

        if (!conflicts.isEmpty()) {
            throw new GloboxApplicationException("时间段与现有日程冲突: " + conflicts.get(0).getConflictReason());
        }

        // 计算时长
        long minutes = ChronoUnit.MINUTES.between(dto.getStartTime(), dto.getEndTime());

        // 创建自定义日程
        CoachCustomSchedule schedule = new CoachCustomSchedule();
        schedule.setCoachUserId(dto.getCoachUserId());
        schedule.setStudentName(dto.getStudentName());
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setDurationMinutes((int) minutes);
        schedule.setVenueName(dto.getVenueName());
        schedule.setVenueAddress(dto.getVenueAddress());
        schedule.setCourseType(dto.getCoachServiceType());
        schedule.setReminderMinutes(dto.getReminderMinutes());
        schedule.setRemark(dto.getRemark());
        schedule.setStatus(1); // 正常
        log.debug("准备插入自定义日程: {}", schedule);
        customScheduleMapper.insert(schedule);
        log.info("自定义日程插入成功 - scheduleId: {}", schedule.getCoachCustomScheduleId());

        // 创建对应的slot record占位
        createRecordForCustomSchedule(schedule);

        log.info("自定义日程创建成功 - scheduleId: {}", schedule.getCoachCustomScheduleId());
        return schedule.getCoachCustomScheduleId();
    }

    /**
     * 验证时间范围合法性
     */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new GloboxApplicationException("开始时间和结束时间不能为空");
        }

        if (!startTime.isBefore(endTime)) {
            throw new GloboxApplicationException("开始时间必须早于结束时间");
        }

//        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
//        if (minutes < 30) {
//            throw new GloboxApplicationException("时长至少为30分钟");
//        }
    }

    /**
     * 检查时间冲突
     */
    private List<CoachSlotConflictVo> checkTimeConflicts(
            Long coachUserId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime) {

        List<CoachSlotConflictVo> conflicts = new ArrayList<>();

        // 1. 检查平台订单冲突
        List<CoachBookings> bookings = bookingsMapper.selectList(
                new LambdaQueryWrapper<CoachBookings>()
                        .eq(CoachBookings::getCoachUserId, coachUserId)
                        .eq(CoachBookings::getCoachBookingDate, date)
                        .in(CoachBookings::getCoachBookingStatus, 2, 3, 4) // 已确认/进行中/已完成
        );

        for (CoachBookings booking : bookings) {
            if (isTimeOverlap(startTime, endTime, booking.getStartTime(), booking.getEndTime())) {
                conflicts.add(CoachSlotConflictVo.builder()
                        .date(date)
                        .startTime(booking.getStartTime())
                        .endTime(booking.getEndTime())
                        .conflictReason("与平台订单冲突")
                        .relatedId(booking.getCoachBookingsId())
                        .build());
            }
        }

        // 2. 检查其他自定义日程冲突
        List<CoachCustomSchedule> existingSchedules = customScheduleMapper.selectList(
                new LambdaQueryWrapper<CoachCustomSchedule>()
                        .eq(CoachCustomSchedule::getCoachUserId, coachUserId)
                        .eq(CoachCustomSchedule::getScheduleDate, date)
                        .eq(CoachCustomSchedule::getStatus, 1) // 正常状态
        );

        for (CoachCustomSchedule schedule : existingSchedules) {
            if (isTimeOverlap(startTime, endTime, schedule.getStartTime(), schedule.getEndTime())) {
                conflicts.add(CoachSlotConflictVo.builder()
                        .date(date)
                        .startTime(schedule.getStartTime())
                        .endTime(schedule.getEndTime())
                        .conflictReason("与自定义日程冲突")
                        .relatedId(schedule.getCoachCustomScheduleId())
                        .build());
            }
        }

        return conflicts;
    }


    /**
     * 更新自定义日程
     *
     * @param scheduleId
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomSchedule(Long scheduleId, CoachCustomScheduleDto dto) {
        log.info("更新自定义日程 - scheduleId: {}", scheduleId);

        CoachCustomSchedule schedule = customScheduleMapper.selectById(scheduleId);
        if (schedule == null || !schedule.getCoachUserId().equals(dto.getCoachUserId())) {
            throw new GloboxApplicationException("日程不存在或无权限");
        }

        // 如果修改了时间,检查冲突
        boolean timeChanged = !schedule.getScheduleDate().equals(dto.getScheduleDate()) ||
                !schedule.getStartTime().equals(dto.getStartTime()) ||
                !schedule.getEndTime().equals(dto.getEndTime());

        if (timeChanged) {
            validateTimeRange(dto.getStartTime(), dto.getEndTime());

            List<CoachSlotConflictVo> conflicts = checkTimeConflicts(
                    dto.getCoachUserId(),
                    dto.getScheduleDate(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    scheduleId);

            if (!conflicts.isEmpty()) {
                throw new GloboxApplicationException("时间段与现有日程冲突: " + conflicts.get(0).getConflictReason());
            }

            // 删除旧的占位记录
            deleteRecordForCustomSchedule(scheduleId);
        }
        // 更新日程
        long minutes = ChronoUnit.MINUTES.between(dto.getStartTime(), dto.getEndTime());
        schedule.setStudentName(dto.getStudentName());
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setDurationMinutes((int) minutes);
        schedule.setVenueName(dto.getVenueName());
        schedule.setVenueAddress(dto.getVenueAddress());
        schedule.setCourseType(dto.getCoachServiceType());
        schedule.setReminderMinutes(dto.getReminderMinutes());
        schedule.setRemark(dto.getRemark());

        customScheduleMapper.updateById(schedule);

        // 如果时间改变,创建新的占位记录
        if (timeChanged) {
            createRecordForCustomSchedule(schedule);
        }
    }


    /**
     * 删除自定义日程(删除占位记录)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomSchedule(Long scheduleId, Long coachUserId) {
        log.info("删除自定义日程 - scheduleId: {}", scheduleId);

        CoachCustomSchedule schedule = customScheduleMapper.selectById(scheduleId);
        if (schedule == null || !schedule.getCoachUserId().equals(coachUserId)) {
            throw new GloboxApplicationException("日程不存在或无权限");
        }

        // 删除日程
        customScheduleMapper.deleteById(scheduleId);

        // 删除对应的占位记录
        deleteRecordForCustomSchedule(scheduleId);

        log.info("自定义日程删除成功");
    }

    /**
     * 检查时段冲突
     */
    @Override
    public List<CoachSlotConflictVo> checkSlotConflicts(
            Long coachUserId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime) {

        List<CoachSlotConflictVo> conflicts = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            List<CoachSlotConflictVo> dateConflicts = checkTimeConflicts(
                    coachUserId, currentDate, startTime, endTime);
            conflicts.addAll(dateConflicts);
            currentDate = currentDate.plusDays(1);
        }

        return conflicts;
    }

    /**
     * 自动生成时段记录(已废弃,采用按需生成)
     */
    @Override
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void generateSlotRecords(Long coachUserId, int days) {
        log.warn("generateSlotRecords已废弃,采用按需生成策略,无需调用此方法");
        // 保留方法签名以兼容接口,实际不执行任何操作
    }

    // ========== 辅助方法 ==========

    /**
     * 按日期组织记录,便于快速查找
     * 返回: Map<日期, Map<模板ID, 记录>>
     */
    private Map<String, Map<Long, CoachSlotRecord>> buildRecordMapByDate(List<CoachSlotRecord> records) {
        Map<String, Map<Long, CoachSlotRecord>> result = new HashMap<>();

        for (CoachSlotRecord record : records) {
            String dateKey = record.getBookingDate().toString();
            result.computeIfAbsent(dateKey, k -> new HashMap<>())
                    .put(record.getCoachSlotTemplateId(), record);
        }

        return result;
    }

    private CoachSlotRecordVo buildSlotRecordVo(CoachSlotTemplate template) {
        return CoachSlotRecordVo.builder()
                .slotRecordId(null)
                .templateId(template.getCoachSlotTemplateId())
                .bookingDate(null)
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .durationMinutes(template.getDurationMinutes())
                .status(null)
                .statusDesc("模板配置")
                .price(template.getPrice())
                .acceptableAreas(parseJsonArrayFromDb(template.getAcceptableAreas()))
                .venueRequirementDesc(template.getVenueRequirementDesc())
                .build();
    }

    private CoachAvailableSlotVo buildAvailableSlotVo(
            CoachSlotTemplate template,
            LocalDate date,
            CoachSlotRecord record) {
        return CoachAvailableSlotVo.builder()
                .coachSlotTemplateId(template.getCoachSlotTemplateId())
                .slotRecordId(record != null ? record.getCoachSlotRecordId() : null)
                .bookingDate(date)
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .durationMinutes(template.getDurationMinutes())
                .price(template.getPrice())
                .acceptableAreas(parseJsonArrayFromDb(template.getAcceptableAreas()))
                .venueRequirementDesc(template.getVenueRequirementDesc())
                .coachServiceType(template.getCoachServiceType())
                .serviceName(getServiceName(template.getCoachServiceType()))
                .build();
    }

    private CoachScheduleVo buildScheduleFromBooking(CoachBookings booking) {
        return CoachScheduleVo.builder()
                .scheduleType("BOOKING")
                .scheduleDate(booking.getCoachBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .studentName(booking.getContactName())
                .venue(booking.getVenueName())
                .coachServiceType(Math.toIntExact(booking.getCoachServiceId()))
                .bookingId(booking.getCoachBookingsId())
                .build();
    }

    private CoachScheduleVo buildScheduleFromCustom(CoachCustomSchedule schedule) {
        return CoachScheduleVo.builder()
                .scheduleType("CUSTOM")
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .studentName(schedule.getStudentName())
                .venue(schedule.getVenueName())
                .coachServiceType(schedule.getCourseType())
                .remark(schedule.getRemark())
                .customScheduleId(schedule.getCoachCustomScheduleId())
                .build();
    }

    private List<CoachSlotConflictVo> checkTimeConflicts(
            Long coachUserId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeScheduleId) {

        List<CoachSlotConflictVo> conflicts = new ArrayList<>();

        // 1. 检查订单冲突
        List<CoachBookings> bookings = bookingsMapper.selectList(
                new LambdaQueryWrapper<CoachBookings>()
                        .eq(CoachBookings::getCoachUserId, coachUserId)
                        .eq(CoachBookings::getCoachBookingDate, date)
                        .in(CoachBookings::getCoachBookingStatus, 2, 3, 4, 5)
                //1-待支付，2-待确认，3-已确认，4-进行中，5-已完成，6-已取消，7-已退款
        );

        for (CoachBookings booking : bookings) {
            if (isTimeOverlap(startTime, endTime, booking.getStartTime(), booking.getEndTime())) {
                conflicts.add(CoachSlotConflictVo.builder()
                        .date(date)
                        .startTime(booking.getStartTime())
                        .endTime(booking.getEndTime())
                        .conflictReason("与订单冲突")
                        .relatedId(booking.getCoachBookingsId())
                        .build());
            }
        }

        // 2. 检查自定义日程冲突
        List<CoachCustomSchedule> schedules = customScheduleMapper.selectList(
                new LambdaQueryWrapper<CoachCustomSchedule>()
                        .eq(CoachCustomSchedule::getCoachUserId, coachUserId)
                        .eq(CoachCustomSchedule::getScheduleDate, date)
                        .eq(CoachCustomSchedule::getStatus, 1)
        );

        for (CoachCustomSchedule schedule : schedules) {
            if (schedule.getCoachCustomScheduleId().equals(excludeScheduleId)) {
                continue;
            }
            if (isTimeOverlap(startTime, endTime, schedule.getStartTime(), schedule.getEndTime())) {
                conflicts.add(CoachSlotConflictVo.builder()
                        .date(date)
                        .startTime(schedule.getStartTime())
                        .endTime(schedule.getEndTime())
                        .conflictReason("与自定义日程冲突")
                        .relatedId(schedule.getCoachCustomScheduleId())
                        .build());
            }
        }

        return conflicts;
    }

    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }


    /**
     * 创建自定义日程占位记录
     * 修正：状态码使用枚举
     */
    private void createRecordForCustomSchedule(CoachCustomSchedule schedule) {
        CoachSlotRecord record = new CoachSlotRecord();
        record.setCoachUserId(schedule.getCoachUserId());
        record.setBookingDate(schedule.getScheduleDate());
        record.setCoachSlotRecordId(null);
        record.setStartTime(schedule.getStartTime());
        record.setEndTime(schedule.getEndTime());
        record.setStatus(CoachSlotRecordStatusEnum.CUSTOM_EVENT.getCode()); // 自定义日程
        record.setLockedType(CoachSlotLockType.CUSTOM_SCHEDULE_LOCK.getCode()); // 自定义日程锁定
        record.setCustomScheduleId(schedule.getCoachCustomScheduleId());
        record.setOperatorSource(1);
        log.debug("准备插入自定义记录: {}", record);
        slotRecordMapper.insert(record);
        log.debug("插入后的ID: {}", record.getCoachSlotRecordId());
    }


    private void deleteRecordForCustomSchedule(Long scheduleId) {
        slotRecordMapper.delete(
                new LambdaQueryWrapper<CoachSlotRecord>()
                        .eq(CoachSlotRecord::getCustomScheduleId, scheduleId)
        );
    }

    private void logBatchOperation(
            Long coachUserId,
            int operationType,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            int affectedCount,
            String reason,
            Long operatorId) {

        CoachSlotBatchOperationLog log = CoachSlotBatchOperationLog.builder()
                .coachUserId(coachUserId)
                .operationType(operationType)
                .dateRangeStart(startDate)
                .dateRangeEnd(endDate)
                .timeRangeStart(startTime)
                .timeRangeEnd(endTime)
                .affectedCount(affectedCount)
                .operationReason(reason)
                .operatorId(operatorId)
                .build();

        batchLogMapper.insert(log);
    }

    private String getServiceName(Integer serviceType) {
        if (serviceType == null) {
            return "不限";
        }

        try {
            CoachServiceTypeEnum serviceTypeEnum = CoachServiceTypeEnum.fromValue(serviceType);
            return serviceTypeEnum.getDescription();
        } catch (IllegalArgumentException e) {
            return "未知";
        }
    }

    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("JSON转换失败", e);
            return "[]";
        }
    }

    private List<String> parseJsonArrayFromDb(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(jsonStr, List.class);
        } catch (JsonProcessingException e) {
            log.warn("解析JSON数组失败: {}", jsonStr, e);
            return Collections.emptyList();
        }
    }
}