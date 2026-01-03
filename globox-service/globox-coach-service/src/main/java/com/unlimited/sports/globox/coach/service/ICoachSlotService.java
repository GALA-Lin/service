package com.unlimited.sports.globox.coach.service;

import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.vo.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @since 2026/1/3 13:15
 * 教练时段管理服务接口
 */

public interface ICoachSlotService {

    /**
     * 初始化教练时段模板
     * 教练首次设置可用时段，批量创建模板
     *
     * @param dto 时段模板创建请求
     * @return 创建成功的模板数量
     */
    Integer initializeSlotTemplates(CoachSlotTemplateInitDto dto);

    /**
     * 更新时段模板
     *
     * @param templateId 模板ID
     * @param dto 更新内容
     * @return 是否成功
     */
    Boolean updateSlotTemplate(Long templateId, CoachSlotTemplateUpdateDto dto);

    /**
     * 删除时段模板
     * 软删除，已关联的记录保留
     *
     * @param coachUserId 教练ID
     * @param templateId 模板ID
     * @return 是否成功
     */
    Boolean deleteSlotTemplate(Long coachUserId, Long templateId);


    /**
     * 批量锁定时段
     * 教练手动锁定某些时段（休假、私人事务等）
     *
     * @param dto 批量锁定请求
     * @return 锁定的记录数量
     */
    Integer batchLockSlots(CoachSlotBatchLockDto dto);

    /**
     * 批量解锁时段
     *
     * @param dto 批量解锁请求
     * @return 解锁的记录数量
     */
    Integer batchUnlockSlots(CoachSlotBatchUnlockDto dto);

    /**
     * 查询教练某日的时段列表
     *
     * @param coachUserId 教练ID
     * @param date 日期
     * @return 时段列表
     */
    List<CoachSlotRecordVo> getSlotsByDate(Long coachUserId, LocalDate date);

    /**
     * 查询教练日程表（包含平台订单和自定义日程）
     *
     * @param dto 查询条件
     * @return 日程列表
     */
    List<CoachScheduleVo> getCoachSchedule(CoachScheduleQueryDto dto);

    /**
     * 添加自定义日程
     * 检测时间冲突，无冲突时创建并占用对应时段
     *
     * @param dto 自定义日程
     * @return 日程ID
     */
    Long addCustomSchedule(CoachCustomScheduleDto dto);

    /**
     * 更新自定义日程
     *
     * @param scheduleId 日程ID
     * @param dto 更新内容
     * @return 是否成功
     */
    Boolean updateCustomSchedule(Long scheduleId, CoachCustomScheduleDto dto);

    /**
     * 删除自定义日程
     * 同时释放占用的时段
     *
     * @param coachUserId 教练ID
     * @param scheduleId 日程ID
     * @return 是否成功
     */
    Boolean deleteCustomSchedule(Long coachUserId, Long scheduleId);

    /**
     * 检测时间冲突
     *
     * @param coachUserId 教练ID
     * @param date 日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeScheduleId 排除的日程ID（用于更新时检测）
     * @return 冲突的时段列表
     */
    List<CoachSlotConflictVo> checkTimeConflict(
            Long coachUserId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeScheduleId
    );

    /**
     * 用户锁定时段（下单流程）
     *
     * @param dto 锁定请求
     * @return 是否成功
     */
    Boolean lockSlotForBooking(CoachSlotLockDto dto);

    /**
     * 释放锁定的时段
     * 用户取消下单或支付超时
     *
     * @param slotRecordId 时段记录ID
     * @return 是否成功
     */
    Boolean releaseLockedSlot(Long slotRecordId);

    /**
     * 确认预约（支付成功后）
     * 将锁定状态改为已预约
     *
     * @param slotRecordId 时段记录ID
     * @param bookingId 订单ID
     * @return 是否成功
     */
    Boolean confirmBooking(Long slotRecordId, Long bookingId);

    /**
     * 取消预约
     * 将已预约时段释放为可用
     *
     * @param bookingId 订单ID
     * @return 是否成功
     */
    Boolean cancelBooking(Long bookingId);

    /**
     * 查询可预约时段（用户视角）
     *
     * @param dto 查询条件
     * @return 可预约时段列表
     */
    List<CoachAvailableSlotVo> getAvailableSlots(CoachAvailableSlotQueryDto dto);

    /**
     * 定时任务：释放过期锁定
     * 扫描所有锁定超时的时段，自动释放
     *
     * @return 释放的记录数量
     */
    Integer releaseExpiredLocks();

    /**
     * 定时任务：自动生成未来时段
     * 根据advance_booking_days自动生成可预约时段
     *
     * @return 生成的记录数量
     */
    Integer autoGenerateFutureSlots();
}
