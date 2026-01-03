package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.vo.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * @since 2026/1/3 13:15
 * 教练时段管理服务接口
 */
public interface ICoachSlotService {

    /**
     * 初始化时段模板
     * 批量创建教练的时段模板
     */
    void initSlotTemplates(CoachSlotTemplateInitDto dto);

    /**
     * 更新时段模板
     */
    void updateSlotTemplate(Long templateId, CoachSlotTemplateUpdateDto dto);

    /**
     * 删除时段模板
     * 软删除，不影响已生成的记录
     */
    void deleteSlotTemplate(Long templateId, Long coachUserId);

    /**
     * 查询教练的所有时段模板
     */
    List<CoachSlotRecordVo> getSlotTemplates(Long coachUserId);

    /**
     * 查询可预约时段
     * 返回指定日期范围内可预约的时段（AVAILABLE状态）
     */
    Map<String, List<CoachAvailableSlotVo>> getAvailableSlots(CoachAvailableSlotQueryDto dto);

    /**
     * 查询教练日程
     * 包含订单和自定义日程
     */
    List<CoachScheduleVo> getCoachSchedule(CoachScheduleQueryDto dto);

    /**
     * 锁定时段（用户下单）
     * 返回是否锁定成功
     */
    boolean lockSlot(CoachSlotLockDto dto);

    /**
     * 解锁时段（取消订单）
     */
    void unlockSlot(Long slotRecordId, Long userId);

    /**
     * 批量锁定时段（教练端操作）
     * 返回成功锁定的数量
     */
    int batchLockSlots(CoachSlotBatchLockDto dto);

    /**
     * 批量解锁时段（教练端操作）
     * 返回成功解锁的数量
     */
    int batchUnlockSlots(CoachSlotBatchUnlockDto dto);

    /**
     * 创建自定义日程
     * 返回冲突列表，如果有冲突则不创建
     */
    List<CoachSlotConflictVo> createCustomSchedule(CoachCustomScheduleDto dto);

    /**
     * 更新自定义日程
     */
    void updateCustomSchedule(Long scheduleId, CoachCustomScheduleDto dto);

    /**
     * 删除自定义日程
     */
    void deleteCustomSchedule(Long scheduleId, Long coachUserId);

    /**
     * 检查时段冲突
     * 返回冲突的时段列表
     */
    List<CoachSlotConflictVo> checkSlotConflicts(
            Long coachUserId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    );

    /**
     * 自动生成未来N天的时段记录
     * 根据时段模板自动生成
     */
    void generateSlotRecords(Long coachUserId, int days);
}