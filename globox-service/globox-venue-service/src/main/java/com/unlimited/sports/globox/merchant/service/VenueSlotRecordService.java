package com.unlimited.sports.globox.merchant.service;

import com.unlimited.sports.globox.model.merchant.vo.SlotAvailabilityVo;
import com.unlimited.sports.globox.model.merchant.vo.SlotGenerationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.VenueSlotAvailabilityVo;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @since 2025/12/27 15:20
 * 订场槽位记录管理服务接口
 */

public interface VenueSlotRecordService {

    /**
     * 为指定日期生成槽位记录
     * @param courtId 场地ID
     * @param date 日期
     * @param overwrite 是否覆盖已有记录
     * @return 生成结果
     */
    SlotGenerationResultVo generateRecordsForDate(Long courtId, LocalDate date, boolean overwrite);

    /**
     * 批量生成日期范围的槽位记录
     * @param courtId 场地ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param overwrite 是否覆盖
     * @return 生成结果
     */
    SlotGenerationResultVo generateRecordsForDateRange(Long courtId, LocalDate startDate,
                                                       LocalDate endDate, boolean overwrite);

    /**
     * 查询某天的时段可用性
     * @param courtId 场地ID
     * @param date 日期
     * @return 时段列表
     */
    List<SlotAvailabilityVo> queryAvailability(Long courtId, LocalDate date);

    List<VenueSlotAvailabilityVo> queryVenueAvailability(@NotNull Long venueId, LocalDate date, LocalTime startTime, LocalTime endTime);
}
