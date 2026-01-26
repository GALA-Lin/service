package com.unlimited.sports.globox.model.venue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 预订预览请求DTO
 * 用于在下单前预览订单信息和价格
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GeneralBookingPreviewRequestDto {

    /**
     * 预定日期
     */
    @NotNull(message = "预定日期不能为空")
    private LocalDate bookingDate;

    /**
     * 预定的槽位ID列表
     */
    @NotEmpty(message = "预定槽位不能为空")
    private List<Long> slotIds;

    /**
     * 用户纬度（用于计算距离，可选）
     */
    private Double latitude;

    /**
     * 用户经度（用于计算距离，可选）
     */
    private Double longitude;
}
