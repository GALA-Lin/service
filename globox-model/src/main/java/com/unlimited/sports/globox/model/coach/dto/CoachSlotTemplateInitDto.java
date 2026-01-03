package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * @since 2026/1/3 13:17
 * 时段模板初始化DTO
 */
@Data
public class CoachSlotTemplateInitDto {

    /**
     * 教练ID
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

    /**
     * 时段大小（分钟），默认60分钟
     */
    @NotNull(message = "时段大小不能为空")
    @Min(value = 30, message = "时段大小最小30分钟")
    @Max(value = 180, message = "时段大小最大180分钟")
    private Integer slotDurationMinutes = 60;

    /**
     * 时段列表
     */
    @NotEmpty(message = "时段列表不能为空")
    @Size(max = 20, message = "一次最多创建20个时段")
    private List<SlotTemplateItem> slots;

    @Data
    public static class SlotTemplateItem {
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
         * 服务类型:0-无限制，1-一对一教学，2-一对一陪练，3-一对二，4-小班(3-6人)
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
     * 提前开放预约天数，默认7天
     */
    @Min(value = 1, message = "至少提前1天开放")
    @Max(value = 90, message = "最多提前90天开放")
    private Integer advanceBookingDays = 7;
}

