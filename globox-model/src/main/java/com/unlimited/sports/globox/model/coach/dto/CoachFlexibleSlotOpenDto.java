package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 灵活时段开放DTO
 * 支持三种开放模式：
 * 1. 单次开放：指定日期开放时段
 * 2. 批量开放：多个日期开放相同时段
 * 3. 长期模板：创建重复时段模板
 */
@Data
public class CoachFlexibleSlotOpenDto {

    /**
     * 教练ID
     */
    private Long coachUserId;

    /**
     * 开放模式
     * SINGLE: 单次开放（指定单个日期）
     * BATCH: 批量开放（指定日期列表）
     * TEMPLATE: 长期模板（按周重复）
     */
    @NotNull(message = "开放模式不能为空")
    private OpenMode openMode;

    /**
     * 单次开放/模板生效日期
     */
    private LocalDate singleDate;

    /**
     * 批量开放日期列表（最多100个）
     */
    @Size(max = 100, message = "批量开放日期不能超过100个")
    private List<LocalDate> batchDates;

    /**
     * 时段列表（支持一次开放多个时段）
     */
    @NotEmpty(message = "时段列表不能为空")
    @Size(max = 20, message = "一次最多开放20个时段")
    private List<SlotItem> slots;

    /**
     * 模板模式：重复类型（仅TEMPLATE模式需要）
     * WEEKLY: 按周重复
     */
    private RepeatType repeatType;

    /**
     * 模板模式：重复的星期几（1=周一，7=周日）
     */
    @Size(max = 7, message = "最多选择7天")
    private List<Integer> repeatWeekDays;

    /**
     * 模板模式：提前开放天数
     */
    @Min(value = 1, message = "至少提前1天开放")
    @Max(value = 90, message = "最多提前90天开放")
    private Integer advanceBookingDays;

    /**
     * 时段项
     */
    @Data
    public static class SlotItem {
        /**
         * 开始时间
         */
        @NotNull(message = "开始时间不能为空")
        private LocalTime startTime;

        /**
         * 结束时间
         */
        @NotNull(message = "结束时间不能为空")
        private LocalTime endTime;

        /**
         * 服务类型
         */
        private Integer coachServiceType;

        /**
         * 价格
         */
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于0")
        private BigDecimal price;

        /**
         * 可接受区域
         */
        private List<String> acceptableAreas;

        /**
         * 场地要求说明
         */
        private String venueRequirementDesc;
    }

    /**
     * 开放模式枚举
     */
    public enum OpenMode {
        SINGLE,   // 单次开放
        BATCH,    // 批量开放
        TEMPLATE  // 长期模板
    }

    /**
     * 重复类型枚举
     */
    public enum RepeatType {
        WEEKLY  // 按周重复
    }
}