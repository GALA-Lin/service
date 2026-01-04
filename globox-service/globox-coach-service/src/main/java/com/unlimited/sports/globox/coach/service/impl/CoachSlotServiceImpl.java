package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.coach.mapper.*;
import com.unlimited.sports.globox.coach.service.ICoachSlotService;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.entity.*;
import com.unlimited.sports.globox.model.coach.enums.CoachServiceTypeEnum;
import com.unlimited.sports.globox.model.coach.vo.CoachAvailableSlotVo;
import com.unlimited.sports.globox.model.coach.vo.CoachScheduleVo;
import com.unlimited.sports.globox.model.coach.vo.CoachSlotConflictVo;
import lombok.extern.slf4j.Slf4j;
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
 * @since 2026/1/3 15:35
 *
 */
@Slf4j
@Service
public class CoachSlotServiceImpl implements ICoachSlotService {

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

    /**
     * 初始化时段模板
     * 批量创建教练的时段模板
     *
     * @param dto 初始化时段模板Dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initSlotTemplates(CoachSlotTemplateInitDto dto) {
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

        // 批量创建模板
        List<CoachSlotTemplate> templates = dto.getSlots().stream()
                .map(item -> {
                    CoachSlotTemplate template = new CoachSlotTemplate();
                    template.setCoachUserId(dto.getCoachUserId());
                    template.setStartTime(item.getStartTime());
                    template.setEndTime(item.getEndTime());
                    template.setDurationMinutes(dto.getSlotDurationMinutes());
                    template.setCoachServiceId(item.getCoachServiceType());
                    template.setPrice(item.getPrice());
                    template.setAcceptableAreas(item.getAcceptableAreas().toString());
                    template.setVenueRequirementDesc(item.getVenueRequirementDesc());
                    template.setAdvanceBookingDays(dto.getAdvanceBookingDays());
                    template.setIsDeleted(0);
                    return template;
                })
                .toList();

        templates.forEach(slotTemplateMapper::insert);

        // 自动生成未来N天的记录
        generateSlotRecords(dto.getCoachUserId(), dto.getAdvanceBookingDays());

        log.info("时段模板初始化完成 - coachUserId: {}", dto.getCoachUserId());
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
     * 查询可预约时段
     * 返回指定日期范围内可预约的时段（AVAILABLE状态）
     *
     * @param dto
     */
    /**
     * 查询可预约时段
     */
    @Override
    public List<CoachAvailableSlotVo> getAvailableSlots(CoachAvailableSlotQueryDto dto) {
        log.info("查询可预约时段 - coachUserId: {}, 日期范围: {} - {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        // 查询所有模板
        LambdaQueryWrapper<CoachSlotTemplate> templateWrapper = new LambdaQueryWrapper<CoachSlotTemplate>()
                .eq(CoachSlotTemplate::getCoachUserId, dto.getCoachUserId())
                .eq(CoachSlotTemplate::getIsDeleted, 0);

        if (dto.getCoachServiceType() != null) {
            templateWrapper.and(w -> w.eq(CoachSlotTemplate::getCoachServiceId, dto.getCoachServiceType())
                    .or().isNull(CoachSlotTemplate::getCoachServiceId));
        }

        List<CoachSlotTemplate> templates = slotTemplateMapper.selectList(templateWrapper);
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }

        List<CoachAvailableSlotVo> availableSlots = new ArrayList<>();

        // 遍历日期范围
        LocalDate currentDate = dto.getStartDate();
        while (!currentDate.isAfter(dto.getEndDate())) {
            LocalDate finalCurrentDate = currentDate;

            // 查询当天已存在的记录
            List<CoachSlotRecord> existingRecords = slotRecordMapper.selectList(
                    new LambdaQueryWrapper<CoachSlotRecord>()
                            .eq(CoachSlotRecord::getCoachUserId, dto.getCoachUserId())
                            .eq(CoachSlotRecord::getBookingDate, currentDate)
            );

            Map<Long, CoachSlotRecord> recordMap = existingRecords.stream()
                    .collect(Collectors.toMap(
                            CoachSlotRecord::getCoachSlotTemplateId,
                            r -> r,
                            (r1, r2) -> r1));

            // 遍历模板生成可用时段
            for (CoachSlotTemplate template : templates) {
                // 检查是否在提前预约天数范围内
                long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), finalCurrentDate);
                if (daysUntil > template.getAdvanceBookingDays()) {
                    continue;
                }

                CoachSlotRecord record = recordMap.get(template.getCoachSlotTemplateId());

                // 只返回可用时段（没有记录或状态为AVAILABLE的）
                if (record == null) {
                    availableSlots.add(buildAvailableSlotVo(template, finalCurrentDate, null));
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("找到可预约时段数: {}", availableSlots.size());
        return availableSlots;
    }

    /**
     * 锁定时段（用户下单）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSlot(CoachSlotLockDto dto) {
        log.info("锁定时段 - slotRecordId: {}, userId: {}",
                dto.getSlotRecordId(), dto.getUserId());

        CoachSlotRecord record = slotRecordMapper.selectById(dto.getSlotRecordId());
        if (record == null) {
            throw new GloboxApplicationException("时段不存在");
        }

        // 检查是否已被占用
        if (record.getStatus() != 1) { // 1=AVAILABLE
            throw new GloboxApplicationException("该时段已被占用或不可预约");
        }

        // 锁定时段
        record.setStatus(1); // LOCKED
        record.setLockedByUserId(dto.getUserId());
        record.setLockedType(1); // 用户下单锁定
        record.setLockedUntil(LocalDateTime.now().plusMinutes(dto.getLockMinutes()));
        record.setOperatorId(dto.getUserId());
        record.setOperatorSource(2); // 用户端

        int updated = slotRecordMapper.updateById(record);
        if (updated == 0) {
            throw new GloboxApplicationException("锁定失败，请重试");
        }

        log.info("时段锁定成功");
    }

    /**
     * 批量锁定时段（教练手动锁定）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchLockSlots(CoachSlotBatchLockDto dto) {
        log.info("批量锁定时段 - coachUserId: {}, 日期范围: {} - {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        // 查询需要锁定的记录
        LambdaQueryWrapper<CoachSlotRecord> wrapper = new LambdaQueryWrapper<CoachSlotRecord>()
                .eq(CoachSlotRecord::getCoachUserId, dto.getCoachUserId())
                .between(CoachSlotRecord::getBookingDate, dto.getStartDate(), dto.getEndDate())
                .eq(CoachSlotRecord::getStatus, 1); // 只锁定AVAILABLE状态

        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            wrapper.ge(CoachSlotRecord::getStartTime, dto.getStartTime())
                    .le(CoachSlotRecord::getEndTime, dto.getEndTime());
        }

        List<CoachSlotRecord> records = slotRecordMapper.selectList(wrapper);

        // 批量更新
        int updatedCount = 0;
        for (CoachSlotRecord record : records) {
            record.setStatus(2); // UNAVAILABLE
            record.setLockedType(2); // 教练手动锁定
            record.setLockReason(dto.getLockReason());
            record.setOperatorId(dto.getOperatorId());
            record.setOperatorSource(1); // 教练端

            slotRecordMapper.updateById(record);
            updatedCount++;
        }

        // 记录操作日志
        logBatchOperation(dto.getCoachUserId(), 2,
                dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(),
                updatedCount, dto.getLockReason(), dto.getOperatorId());

        log.info("批量锁定完成 - 影响记录数: {}", updatedCount);
        return updatedCount;
    }

    /**
     * 批量解锁时段
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchUnlockSlots(CoachSlotBatchUnlockDto dto) {
        log.info("批量解锁时段 - coachUserId: {}, 日期范围: {} - {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        // 查询需要解锁的记录
        LambdaQueryWrapper<CoachSlotRecord> wrapper = new LambdaQueryWrapper<CoachSlotRecord>()
                .eq(CoachSlotRecord::getCoachUserId, dto.getCoachUserId())
                .between(CoachSlotRecord::getBookingDate, dto.getStartDate(), dto.getEndDate())
                .eq(CoachSlotRecord::getStatus, 2) // 只解锁UNAVAILABLE状态
                .eq(CoachSlotRecord::getLockedType, 2); // 只解锁手动锁定的

        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            wrapper.ge(CoachSlotRecord::getStartTime, dto.getStartTime())
                    .le(CoachSlotRecord::getEndTime, dto.getEndTime());
        }

        List<CoachSlotRecord> records = slotRecordMapper.selectList(wrapper);

        // 批量更新
        int updatedCount = 0;
        for (CoachSlotRecord record : records) {
            // 删除记录以恢复可用状态
            slotRecordMapper.deleteById(record.getCoachSlotRecordId());
            updatedCount++;
        }

        // 记录操作日志
        logBatchOperation(dto.getCoachUserId(), 3,
                dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(),
                updatedCount, "批量解锁", dto.getOperatorId());

        log.info("批量解锁完成 - 影响记录数: {}", updatedCount);
        return updatedCount;
    }

    /**
     * 创建自定义日程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CoachSlotConflictVo> createCustomSchedule(CoachCustomScheduleDto dto) {
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

        // 创建自定义日程
        long minutes = ChronoUnit.MINUTES.between(dto.getStartTime(), dto.getEndTime());

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

        customScheduleMapper.insert(schedule);

        // 创建对应的slot record占位
        createRecordForCustomSchedule(schedule);

        log.info("自定义日程创建成功 - scheduleId: {}", schedule.getCoachCustomScheduleId());
        return schedule;
    }
    /**
     * 查询教练日程
     * 包含订单和自定义日程
     *
     * @param dto
     */
    @Override
    public List<CoachScheduleVo> getCoachSchedule(CoachScheduleQueryDto dto) {
        log.info("查询教练日程 - coachUserId: {}, {} 至 {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        List<CoachScheduleVo> schedules = new ArrayList<>();

        // 1. 查询订单日程
        List<CoachBookings> bookings = bookingsMapper.selectList(
                new LambdaQueryWrapper<CoachBookings>()
                        .eq(CoachBookings::getCoachUserId, dto.getCoachUserId())
                        .between(CoachBookings::getCoachBookingDate,
                                dto.getStartDate(), dto.getEndDate())
                        .in(CoachBookings::getCoachBookingStatus, 2, 3, 4) // 已确认/进行中/已完成
        );

        schedules.addAll(bookings.stream()
                .map(this::buildScheduleFromBooking)
                .toList());

        // 2. 查询自定义日程
        if (dto.getIncludeCustomSchedule()) {
            List<CoachCustomSchedule> customSchedules =
                    customScheduleMapper.selectByDateRange(
                            dto.getCoachUserId(),
                            dto.getStartDate(),
                            dto.getEndDate()
                    );

            schedules.addAll(customSchedules.stream()
                    .map(this::buildScheduleFromCustom)
                    .toList());
        }

        // 按日期和时间排序
        schedules.sort(Comparator
                .comparing(CoachScheduleVo::getScheduleDate)
                .thenComparing(CoachScheduleVo::getStartTime));

        return schedules;
    }

    /**
     * 锁定时段（用户下单）
     * 返回是否锁定成功
     *
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean lockSlot(CoachSlotLockDto dto) {
        log.info("锁定时段 - slotRecordId: {}, userId: {}",
                dto.getSlotRecordId(), dto.getUserId());

        LocalDateTime lockedUntil = LocalDateTime.now()
                .plusMinutes(dto.getLockMinutes());

        int updated = slotRecordMapper.updateLockIfAvailable(
                dto.getSlotRecordId(),
                2, // LOCKED
                dto.getUserId(),
                lockedUntil
        );

        if (updated > 0) {
            log.info("时段锁定成功 - slotRecordId: {}", dto.getSlotRecordId());
            return true;
        } else {
            log.warn("时段锁定失败，可能已被占用 - slotRecordId: {}",
                    dto.getSlotRecordId());
            return false;
        }
    }

    /**
     * 解锁时段（取消订单）
     *
     * @param slotRecordId
     * @param userId
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

        record.setStatus(1); // AVAILABLE
        record.setLockedByUserId(null);
        record.setLockedUntil(null);
        record.setLockedType(null);
        record.setLockReason(null);

        slotRecordMapper.updateById(record);
        log.info("时段解锁成功 - slotRecordId: {}", slotRecordId);
    }

    /**
     * 批量锁定时段（教练端操作）
     * 返回成功锁定的数量
     *
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchLockSlots(CoachSlotBatchLockDto dto) {
        log.info("批量锁定时段 - coachUserId: {}, {} 至 {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());

        // 查询需要锁定的记录
        List<CoachSlotRecord> records = slotRecordMapper.selectByDateRange(
                dto.getCoachUserId(),
                dto.getStartDate(),
                dto.getEndDate()
        );

        // 过滤：只锁定AVAILABLE状态的
        List<CoachSlotRecord> availableRecords = records.stream()
                .filter(r -> r.getStatus() == 1)
                .filter(r -> {
                    // 如果指定了时间范围，进一步过滤
                    if (dto.getStartTime() != null && dto.getEndTime() != null) {
                        return !r.getStartTime().isBefore(dto.getStartTime()) &&
                                !r.getEndTime().isAfter(dto.getEndTime());
                    }
                    return true;
                })
                .toList();

        int lockedCount = 0;
        for (CoachSlotRecord record : availableRecords) {
            record.setStatus(2); // LOCKED
            record.setLockedType(2); // 教练手动锁定
            record.setLockReason(dto.getLockReason());
            record.setOperatorId(dto.getOperatorId());
            record.setOperatorSource(1); // 教练端
            slotRecordMapper.updateById(record);
            lockedCount++;
        }

        // 记录操作日志
        logBatchOperation(dto.getCoachUserId(), 2, // 批量锁定
                dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(),
                lockedCount, dto.getLockReason(), dto.getOperatorId());

        log.info("批量锁定完成 - 成功锁定: {} 个时段", lockedCount);
        return lockedCount;
    }
    /**
     * 批量解锁时段（教练端操作）
     * 返回成功解锁的数量
     *
     * @param dto
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

        // 过滤：只解锁教练手动锁定的
        List<CoachSlotRecord> lockedRecords = records.stream()
                .filter(r -> r.getStatus() == 2 && r.getLockedType() == 2)
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
            record.setStatus(1); // AVAILABLE
            record.setLockedType(null);
            record.setLockReason(null);
            record.setOperatorId(dto.getOperatorId());
            slotRecordMapper.updateById(record);
            unlockedCount++;
        }

        logBatchOperation(dto.getCoachUserId(), 3, // 批量解锁
                dto.getStartDate(), dto.getEndDate(),
                dto.getStartTime(), dto.getEndTime(),
                unlockedCount, "批量解锁", dto.getOperatorId());

        log.info("批量解锁完成 - 成功解锁: {} 个时段", unlockedCount);
        return unlockedCount;
    }



    /**
     * 自动生成时段记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateSlotRecords(Long coachUserId, int days) {
        log.info("生成时段记录 - coachUserId: {}, days: {}", coachUserId, days);

        List<CoachSlotTemplate> templates =
                slotTemplateMapper.selectByCoachUserId(coachUserId);

        if (templates.isEmpty()) {
            log.warn("教练没有时段模板 - coachUserId: {}", coachUserId);
            return;
        }

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(days);

        int generatedCount = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            for (CoachSlotTemplate template : templates) {
                // 检查是否已存在
                CoachSlotRecord existing = slotRecordMapper.selectByTemplateIdAndDate(
                        template.getCoachSlotTemplateId(), date);

                if (existing == null) {
                    CoachSlotRecord record = new CoachSlotRecord();
                    record.setCoachSlotTemplateId(template.getCoachSlotTemplateId());
                    record.setCoachUserId(coachUserId);
                    record.setBookingDate(date);
                    record.setStartTime(template.getStartTime());
                    record.setEndTime(template.getEndTime());
                    record.setStatus(1); // AVAILABLE
                    record.setOperatorSource(3); // 系统自动

                    slotRecordMapper.insert(record);
                    generatedCount++;
                }
            }
        }

        log.info("时段记录生成完成 - 新增: {} 条", generatedCount);
    }

    // ========== 辅助方法 ==========

    private CoachAvailableSlotVo buildAvailableSlotVo(
            CoachSlotTemplate template,
            LocalDate date,
            CoachSlotRecord record) {
        return CoachAvailableSlotVo.builder()
                .slotRecordId(record != null ? record.getCoachSlotRecordId() : null)
                .bookingDate(date)
                .startTime(template.getStartTime())
                .endTime(template.getEndTime())
                .durationMinutes(template.getDurationMinutes())
                .price(template.getPrice())
                .acceptableAreas(Collections.singletonList(template.getAcceptableAreas()))
                .venueRequirementDesc(template.getVenueRequirementDesc())
                .coachServiceType(template.getCoachServiceId())
                .serviceName(getServiceName(template.getCoachServiceId()))
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
}
