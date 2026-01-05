package com.unlimited.sports.globox.coach.controller;

import com.unlimited.sports.globox.coach.service.ICoachSlotService;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.common.utils.AuthContextHolder;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.vo.CoachAvailableSlotVo;
import com.unlimited.sports.globox.model.coach.vo.CoachScheduleVo;
import com.unlimited.sports.globox.model.coach.vo.CoachSlotConflictVo;
import com.unlimited.sports.globox.model.coach.vo.CoachSlotRecordVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * @since 2026/1/3 16:43
 * 教练时段管理接口
 */
@Slf4j
@RestController
@RequestMapping("/coach/slots")
public class CoachSlotController {

    @Autowired
    private ICoachSlotService coachSlotService;

    /**
     * 初始化时段模板(不生成记录)
     */
    @PostMapping("/templates/init")
    public R<Integer> initSlotTemplates(@Valid @RequestBody CoachSlotTemplateInitDto dto) {
        log.info("初始化时段模板 - coachUserId: {}", dto.getCoachUserId());
        Integer result = coachSlotService.initSlotTemplates(dto);
        return R.ok(result);
    }

    /**
     * 查询时段模板列表
     */
    @GetMapping("/templates")
    public R<List<CoachSlotRecordVo>> getSlotTemplates(
            @RequestParam Long coachUserId) {
        log.info("查询时段模板 - coachUserId: {}", coachUserId);
        List<CoachSlotRecordVo> templates = coachSlotService.getSlotTemplates(coachUserId);
        return R.ok(templates);
    }

    /**
     * 更新时段模板
     */
    @PutMapping("/templates/{templateId}")
    public R<Void> updateSlotTemplate(
            @PathVariable Long templateId,
            @Valid @RequestBody CoachSlotTemplateUpdateDto dto) {
        log.info("更新时段模板 - templateId: {}", templateId);
        coachSlotService.updateSlotTemplate(templateId, dto);
        return R.ok();
    }

    /**
     * 删除时段模板
     */
    @DeleteMapping("/templates/{templateId}")
    public R<Void> deleteSlotTemplate(
            @PathVariable Long templateId,
            @RequestParam Long coachUserId) {
        log.info("删除时段模板 - templateId: {}, coachUserId: {}", templateId, coachUserId);
        coachSlotService.deleteSlotTemplate(templateId, coachUserId);
        return R.ok();
    }

    /**
     * 查询可预约时段
     */
    @GetMapping("/available")
    public R<Map<String, List<CoachAvailableSlotVo>>> getAvailableSlots(
            @Valid CoachAvailableSlotQueryDto dto) {
        log.info("查询可预约时段 - coachUserId: {}, {} - {}",
                dto.getCoachUserId(), dto.getStartDate(), dto.getEndDate());
        Map<String, List<CoachAvailableSlotVo>> slots = coachSlotService.getAvailableSlots(dto);
        return R.ok(slots);
    }

    /**
     * 查询教练日程
     */
    @GetMapping("/schedule")
    public R<List<CoachScheduleVo>> getCoachSchedule(@Valid CoachScheduleQueryDto dto) {
        log.info("查询教练日程 - coachUserId: {}", dto.getCoachUserId());
        List<CoachScheduleVo> schedule = coachSlotService.getCoachSchedule(dto);
        return R.ok(schedule);
    }

    /**
     * 锁定时段(用户下单)
     */
    @PostMapping("/lock")
    public R<Boolean> lockSlot(@Valid @RequestBody CoachSlotLockDto dto) {
        log.info("锁定时段 - slotRecordId: {}, userId: {}",
                dto.getSlotRecordId(), dto.getUserId());
        boolean result = coachSlotService.lockSlot(dto);
        return R.ok(result);
    }

    /**
     * 解锁时段(取消订单)
     */
    @PostMapping("/unlock")
    public R<Void> unlockSlot(
            @RequestParam Long slotRecordId,
            @RequestParam Long userId) {
        log.info("解锁时段 - slotRecordId: {}, userId: {}", slotRecordId, userId);
        coachSlotService.unlockSlot(slotRecordId, userId);
        return R.ok();
    }

    /**
     * 批量锁定时段(教练端)
     */
    @PostMapping("/batch-lock")
    public R<Integer> batchLockSlots(@Valid @RequestBody CoachSlotBatchLockDto dto) {
        log.info("批量锁定时段 - coachUserId: {}", dto.getCoachUserId());
        int count = coachSlotService.batchLockSlots(dto);
        return R.ok(count);
    }

    /**
     * 批量解锁时段(教练端)
     */
    @PostMapping("/batch-unlock")
    public R<Integer> batchUnlockSlots(@Valid @RequestBody CoachSlotBatchUnlockDto dto) {
        log.info("批量解锁时段 - coachUserId: {}", dto.getCoachUserId());
        int count = coachSlotService.batchUnlockSlots(dto);
        return R.ok(count);
    }

    /**
     * 创建自定义日程
     */
    @PostMapping("/custom-schedule")
    public R<List<CoachSlotConflictVo>> createCustomSchedule(
            @Valid @RequestBody CoachCustomScheduleDto dto) {
        log.info("创建自定义日程 - coachUserId: {}", dto.getCoachUserId());
        List<CoachSlotConflictVo> conflicts = coachSlotService.createCustomSchedule(dto);
        if (!conflicts.isEmpty()) {
            log.error("创建自定义日程 - 时段冲突: {}", conflicts);
            return R.ok(conflicts);
        }
        return R.ok(conflicts);
    }

    /**
     * 更新自定义日程
     */
    @PutMapping("/custom-schedule/{scheduleId}")
    public R<Void> updateCustomSchedule(
            @PathVariable Long scheduleId,
            @Valid @RequestBody CoachCustomScheduleDto dto) {
        log.info("更新自定义日程 - scheduleId: {}", scheduleId);
        coachSlotService.updateCustomSchedule(scheduleId, dto);
        return R.ok();
    }

    /**
     * 删除自定义日程
     */
    @DeleteMapping("/custom-schedule/{scheduleId}")
    public R<Void> deleteCustomSchedule(
            @PathVariable Long scheduleId,
            @RequestParam Long coachUserId) {
        log.info("删除自定义日程 - scheduleId: {}", scheduleId);
        coachSlotService.deleteCustomSchedule(scheduleId, coachUserId);
        return R.ok();
    }

    /**
     * 检查时段冲突
     */
    @GetMapping("/check-conflicts")
    public R<List<CoachSlotConflictVo>> checkSlotConflicts(
            @RequestParam Long coachUserId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime) {
        log.info("检查时段冲突 - coachUserId: {}", coachUserId);
        List<CoachSlotConflictVo> conflicts = coachSlotService.checkSlotConflicts(
                coachUserId, startDate, endDate, startTime, endTime);
        return R.ok(conflicts);
    }

    /**
     * 手动生成时段记录
     */
    @PostMapping("/generate-records")
    public R<Void> generateSlotRecords(
            @RequestParam Long coachUserId,
            @RequestParam(defaultValue = "30") Integer days) {
        log.info("生成时段记录 - coachUserId: {}, days: {}", coachUserId, days);
        coachSlotService.generateSlotRecords(coachUserId, days);
        return R.ok();
    }
}