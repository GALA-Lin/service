package com.unlimited.sports.globox.coach.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unlimited.sports.globox.coach.mapper.*;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.entity.*;
import com.unlimited.sports.globox.model.coach.enums.CoachSlotRecordStatusEnum;
import com.unlimited.sports.globox.model.coach.vo.CoachAvailableSlotVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.unlimited.sports.globox.model.coach.enums.CoachSlotRecordStatusEnum.getDescription;

/**
 * 优化后的教练时段管理服务
 * 核心设计理念：
 * 1. CoachSlotTemplate: 长期重复的模板（如每周一9:00-10:00）
 * 2. CoachSlotRecord: 具体可预约的时段实例（必须有明确日期）
 * 3. 支持灵活开放：单次/批量/模板三种模式
 */
@Slf4j
@Service
public class CoachSlotServiceOptimizedImpl  {

    @Autowired
    private CoachSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private CoachSlotRecordMapper slotRecordMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoachProfileMapper coachProfileMapper; // 注入教练档案Mapper

    /**
     * 灵活开放时段（新接口）
     *
     * @param dto 开放参数
     * @return 创建的记录数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int openSlotsFlexibly(CoachFlexibleSlotOpenDto dto) {
        log.info("灵活开放时段 - coachUserId: {}, mode: {}",
                dto.getCoachUserId(), dto.getOpenMode());

        return switch (dto.getOpenMode()) {
            case SINGLE -> openSlotsSingle(dto);
            case BATCH -> openSlotsBatch(dto);
            case TEMPLATE -> openSlotsTemplate(dto);
        };
    }

    /**
     * 单次开放：在指定日期创建时段记录
     */
    private int openSlotsSingle(CoachFlexibleSlotOpenDto dto) {
        if (dto.getSingleDate() == null) {
            throw new GloboxApplicationException("单次开放模式必须指定日期");
        }

        List<LocalDate> dates = Collections.singletonList(dto.getSingleDate());
        return createSlotRecordsForDates(dto.getCoachUserId(), dates, dto.getSlots());
    }

    /**
     * 批量开放：在多个指定日期创建时段记录
     */
    private int openSlotsBatch(CoachFlexibleSlotOpenDto dto) {
        if (dto.getBatchDates() == null || dto.getBatchDates().isEmpty()) {
            throw new GloboxApplicationException("批量开放模式必须指定日期列表");
        }

        return createSlotRecordsForDates(dto.getCoachUserId(), dto.getBatchDates(), dto.getSlots());
    }

    /**
     * 模板开放：创建长期重复模板 + 生成近期记录
     */
    private int openSlotsTemplate(CoachFlexibleSlotOpenDto dto) {
        if (dto.getRepeatType() == null || dto.getRepeatWeekDays() == null || dto.getRepeatWeekDays().isEmpty()) {
            throw new GloboxApplicationException("模板模式必须指定重复类型和重复日期");
        }

        int createdCount = 0;

        // 1. 创建时段模板
        for (CoachFlexibleSlotOpenDto.SlotItem slotItem : dto.getSlots()) {
            CoachSlotTemplate template = new CoachSlotTemplate();
            template.setCoachUserId(dto.getCoachUserId());
            template.setStartTime(slotItem.getStartTime());
            template.setEndTime(slotItem.getEndTime());
            template.setDurationMinutes((int) ChronoUnit.MINUTES.between(
                    slotItem.getStartTime(), slotItem.getEndTime()));
            template.setCoachServiceType(slotItem.getCoachServiceType());
            template.setPrice(slotItem.getPrice());
            template.setAcceptableAreas(convertListToJson(slotItem.getAcceptableAreas()));
            template.setVenueRequirementDesc(slotItem.getVenueRequirementDesc());
            template.setAdvanceBookingDays(dto.getAdvanceBookingDays());
            template.setIsDeleted(0);

            slotTemplateMapper.insert(template);
            log.info("创建时段模板 - templateId: {}", template.getCoachSlotTemplateId());
        }

        // 2. 根据模板生成近期可预约记录
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(dto.getAdvanceBookingDays());

        List<LocalDate> applicableDates = generateDatesFromTemplate(
                today, endDate, dto.getRepeatWeekDays());

        createdCount += createSlotRecordsForDates(
                dto.getCoachUserId(), applicableDates, dto.getSlots());

        log.info("模板模式：创建模板 + 生成{}条记录", createdCount);
        return createdCount;
    }

    /**
     * 为指定日期列表创建时段记录
     */
    private int createSlotRecordsForDates(
            Long coachUserId,
            List<LocalDate> dates,
            List<CoachFlexibleSlotOpenDto.SlotItem> slots) {

        int createdCount = 0;

        for (LocalDate date : dates) {
            for (CoachFlexibleSlotOpenDto.SlotItem slotItem : slots) {
                // 检查是否已存在记录：增加 is_deleted = 0 的判断
                Long existingCount = slotRecordMapper.selectCount(
                        new LambdaQueryWrapper<CoachSlotRecord>()
                                .eq(CoachSlotRecord::getCoachUserId, coachUserId)
                                .eq(CoachSlotRecord::getBookingDate, date)
                                .eq(CoachSlotRecord::getStartTime, slotItem.getStartTime())
                                .eq(CoachSlotRecord::getEndTime, slotItem.getEndTime())
                                .eq(CoachSlotRecord::getIsDeleted, 0) // 仅统计未删除的记录
                );

                if (existingCount > 0) {
                    log.warn("时段记录已存在，跳过 - date: {}, time: {}-{}",
                            date, slotItem.getStartTime(), slotItem.getEndTime());
                    continue;
                }

                // 创建新记录
                CoachSlotRecord record = new CoachSlotRecord();
                record.setCoachUserId(coachUserId);
                record.setBookingDate(date);
                record.setStartTime(slotItem.getStartTime());
                record.setEndTime(slotItem.getEndTime());
                record.setStatus(CoachSlotRecordStatusEnum.AVAILABLE.getCode());
                record.setCoachSlotTemplateId(null);
                record.setOperatorId(coachUserId);
                record.setOperatorSource(1); // 教练端
                record.setIsDeleted(0); // 显式初始化为 0

                slotRecordMapper.insert(record);
                createdCount++;
            }
        }
        return createdCount;
    }
    /**
     * 根据模板参数生成日期列表
     */
    private List<LocalDate> generateDatesFromTemplate(
            LocalDate startDate,
            LocalDate endDate,
            List<Integer> weekDays) {

        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            int dayOfWeek = current.getDayOfWeek().getValue(); // 1=周一，7=周日
            if (weekDays.contains(dayOfWeek)) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }

        return dates;
    }

    /**
     * 批量删除时段记录
     * 用于教练取消已开放的时段
     * 只能删除状态为AVAILABLE的记录（未被预约的）
     *
     * @param recordIds 时段记录ID列表
     * @param coachUserId 教练ID（用于权限验证）
     * @return 删除数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchDeleteSlotRecords(List<Long> recordIds, Long coachUserId) {
        if (recordIds == null || recordIds.isEmpty()) {
            return 0;
        }

        log.info("批量软删除时段记录 - coachUserId: {}, recordIds数量: {}",
                coachUserId, recordIds.size());

        int deletedCount = 0;
        List<String> failedRecords = new ArrayList<>();

        for (Long recordId : recordIds) {
            try {
                CoachSlotRecord record = slotRecordMapper.selectById(recordId);

                // 1. 验证记录是否存在（且未被软删除）
                if (record == null || (record.getIsDeleted() != null && record.getIsDeleted() == 1)) {
                    log.warn("时段记录不存在或已被删除 - recordId: {}", recordId);
                    failedRecords.add(recordId + "(不存在)");
                    continue;
                }

                // 2. 验证权限：只能操作自己的记录
                if (!record.getCoachUserId().equals(coachUserId)) {
                    log.warn("无权限操作该记录 - recordId: {}, owner: {}, operator: {}",
                            recordId, record.getCoachUserId(), coachUserId);
                    failedRecords.add(recordId + "(权限不足)");
                    continue;
                }

                // 3. 验证状态：只能删除AVAILABLE状态的记录
                if (!CoachSlotRecordStatusEnum.AVAILABLE.getCode().equals(record.getStatus())) {
                    log.warn("只能删除可用状态的记录 - recordId: {}, status: {}",
                            recordId, record.getStatus());
                    failedRecords.add(recordId + "(状态不允许:" +
                            getDescription(record.getStatus()) + ")");
                    continue;
                }

                // 4. 执行软删除操作
                CoachSlotRecord updateEntity = new CoachSlotRecord();
                updateEntity.setCoachSlotRecordId(recordId);
                updateEntity.setIsDeleted(1);

                int rows = slotRecordMapper.updateById(updateEntity);
                if (rows > 0) {
                    deletedCount++;
                    log.debug("软删除时段记录成功 - recordId: {}", recordId);
                }

            } catch (Exception e) {
                log.error("软删除时段记录失败 - recordId: {}", recordId, e);
                failedRecords.add(recordId + "(异常:" + e.getMessage() + ")");
            }
        }

        // 记录结果日志
        if (deletedCount > 0) {
            log.info("批量软删除完成 - coachUserId: {}, 成功: {}/{}, 失败记录: {}",
                    coachUserId, deletedCount, recordIds.size(),
                    failedRecords.isEmpty() ? "无" : String.join(", ", failedRecords));
        }

        if (!failedRecords.isEmpty()) {
            log.warn("部分记录软删除失败 - coachUserId: {}, 失败详情: {}",
                    coachUserId, failedRecords);
        }

        return deletedCount;
    }

    /**
     * 优化后的查询可预约时段
     * 查询逻辑：
     * 1. 查询日期范围内状态为AVAILABLE的记录
     * 2. 不再依赖模板自动生成
     */
    public Map<String, List<CoachAvailableSlotVo>> getAvailableSlotsOptimized(
            CoachAvailableSlotQueryDto dto) {

        log.info("查询可预约时段（优化版） - coachUserId: {}, 日期范围: {} - {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());
        // 1. 查询教练的配置信息（授课区域与起订时长）
        CoachProfile profile = coachProfileMapper.selectOne(
                new LambdaQueryWrapper<CoachProfile>()
                        .eq(CoachProfile::getCoachUserId, dto.getCoachUserId())
        );
        log.info("教练配置信息 - mainAreas: {}, remoteAreas: {}, minHours: {}, remoteMinHours: {}",
                profile.getCoachServiceArea(), profile.getCoachRemoteServiceArea(),
                profile.getCoachMinHours(), profile.getCoachRemoteMinHours());

        // 处理默认值：如果 profile 为空或字段为 null，则设为默认值
        List<String> mainAreas = (profile != null && profile.getCoachServiceArea() != null)
                ? Collections.singletonList(profile.getCoachServiceArea()) : Collections.emptyList();
        List<String> remoteAreas = (profile != null && profile.getCoachRemoteServiceArea() != null)
                ? profile.getCoachRemoteServiceArea() : Collections.emptyList();

        // 最小授课时间逻辑：为 null 时按照 0 处理
        Integer minHours = (profile != null && profile.getCoachMinHours() != null)
                ? profile.getCoachMinHours() : 0;
        Integer remoteMinHours = (profile != null && profile.getCoachRemoteMinHours() != null)
                ? profile.getCoachRemoteMinHours() : 0;

        // 直接查询日期范围内的可用记录：增加 is_deleted = 0 的判断
        List<CoachSlotRecord> availableRecords = slotRecordMapper.selectList(
                new LambdaQueryWrapper<CoachSlotRecord>()
                        .eq(CoachSlotRecord::getCoachUserId, dto.getCoachUserId())
                        .between(CoachSlotRecord::getBookingDate, dto.getStartDate(), dto.getEndDate())
                        .eq(CoachSlotRecord::getStatus, CoachSlotRecordStatusEnum.AVAILABLE.getCode())
                        .eq(CoachSlotRecord::getIsDeleted, 0) // 过滤掉已删除记录
                        .orderByAsc(CoachSlotRecord::getBookingDate)
                        .orderByAsc(CoachSlotRecord::getStartTime)
        );

        // 2. 按日期分组
        Map<String, List<CoachAvailableSlotVo>> resultMap = new LinkedHashMap<>();

        for (CoachSlotRecord record : availableRecords) {
            String dateKey = record.getBookingDate().toString();

            CoachAvailableSlotVo vo = CoachAvailableSlotVo.builder()
                    .slotRecordId(record.getCoachSlotRecordId())
                    .coachSlotTemplateId(record.getCoachSlotTemplateId())
                    .bookingDate(record.getBookingDate())
                    .startTime(record.getStartTime())
                    .endTime(record.getEndTime())
                    .durationMinutes((int) ChronoUnit.MINUTES.between(
                            record.getStartTime(), record.getEndTime()))
                    .slotStatus(record.getStatus())
                    .slotStatusDesc(getDescription(record.getStatus()))
                    .coachMainServiceAreas(mainAreas)
                    .coachRemoteServiceAreas(remoteAreas)
                    .coachMinHours(minHours)
                    .coachRemoteMinHours(remoteMinHours)
                    .build();

            resultMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(vo);
        }

        log.info("找到可预约日期数: {}, 总时段数: {}",
                resultMap.size(),
                resultMap.values().stream().mapToInt(List::size).sum());

        return resultMap;
    }

    // ========== 辅助方法 ==========

    private String convertListToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("JSON转换失败", e);
            return "[]";
        }
    }
}