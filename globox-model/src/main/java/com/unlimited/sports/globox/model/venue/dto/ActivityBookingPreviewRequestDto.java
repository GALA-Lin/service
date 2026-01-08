package com.unlimited.sports.globox.model.venue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 预订预览请求DTO
 * 用于在下单前预览订单信息和价格
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActivityBookingPreviewRequestDto {

    /**
     * 预定日期
     */
    @NotNull(message = "预定日期不能为空")
    private LocalDate bookingDate;

    /**
     * 预定的槽位ID列表
     */
    @NotNull(message = "预定槽位不能为空")
    @Min(0)
    private Long activityId;

    /**
     * 用户纬度（用于计算距离）
     */
    @NotNull(message = "纬度不能为空")
    private Double latitude;

    /**
     * 用户经度（用于计算距离）
     */
    @NotNull(message = "经度不能为空")
    private Double longitude;
}
