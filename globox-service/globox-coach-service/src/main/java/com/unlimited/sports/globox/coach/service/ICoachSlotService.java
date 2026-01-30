package com.unlimited.sports.globox.coach.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.model.coach.dto.*;
import com.unlimited.sports.globox.model.coach.entity.CoachSlotRecord;
import com.unlimited.sports.globox.model.coach.vo.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 教练时段管理服务接口
 * 核心设计:按需生成记录,节约存储空间
 */
public interface ICoachSlotService  extends IService<CoachSlotRecord> {

    /**
     * 初始化时段模板(不生成记录)
     *
     * @return
     */
    Integer initSlotTemplates(CoachSlotTemplateInitDto dto);

    /**
     * 更新时段模板
     */
    void updateSlotTemplate(Long templateId, CoachSlotTemplateUpdateDto dto);

    /**
     * 删除时段模板
     */
    void deleteSlotTemplate(Long templateId, Long coachUserId);

    /**
     * 查询教练的所有时段模板
     */
    List<CoachSlotRecordVo> getSlotTemplates(Long coachUserId);

    /**
     * 查询可预约时段(按需计算,无需预先生成记录)
     * 返回按日期分组的可用时段
     * 逻辑:无记录或记录状态为AVAILABLE = 可用
     */
    Map<String, List<CoachAvailableSlotVo>> getAvailableSlots(CoachAvailableSlotQueryDto dto);

    /**
     * 查询时段可用性状态(新增)
     * 快速判断时段是否可用,不返回详细信息
     * 返回: Map<"日期_模板ID_时间", Boolean>
     */
    Map<String, Boolean> checkSlotAvailability(CoachAvailableSlotQueryDto dto);

    /**
     * 查询教练日程
     * 包含订单和自定义日程
     */
    List<CoachScheduleVo> getCoachSchedule(CoachScheduleQueryDto dto);

    /**
     * 锁定时段(用户下单,按需生成记录)
     * 返回是否锁定成功
     */
    boolean lockSlot(CoachSlotLockDto dto);

    /**
     * 解锁时段(取消订单,删除记录恢复可用)
     */
    void unlockSlot(Long slotRecordId, Long userId);

    int batchUnlockSlots(List<Long> recordIds, Long userId);

    /**
     * 批量锁定时段(教练端操作,按需生成记录)
     * 返回成功锁定的数量
     */
    int batchLockSlots(CoachSlotBatchLockDto dto);

    /**
     * 批量解锁时段(教练端操作,删除记录恢复可用)
     * 返回成功解锁的数量
     */
    int batchUnlockSlots(CoachSlotBatchUnlockDto dto);

    void updateSlotVenue(UpdateCoachSlotVenueDto dto);

    /**
     * 创建自定义日程(按需生成占位记录)
     * 返回冲突列表,如果有冲突则不创建
     */
    Long createCustomSchedule(CoachCustomScheduleDto dto);

    /**
     * 更新自定义日程
     */
    void updateCustomSchedule(Long scheduleId, CoachCustomScheduleDto dto);

    /**
     * 删除自定义日程(删除占位记录)
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
     * @deprecated 已废弃,采用按需生成策略
     */
    @Deprecated
    void generateSlotRecords(Long coachUserId, int days);
}