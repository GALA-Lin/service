package com.unlimited.sports.globox.model.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

/**
 * 获取订单详情 dto
 */
@Data
@Builder
@NotNull
@AllArgsConstructor
public class GetOrderDetailsDto {

    /**
     * 可空，以路径参数为准
     */
    private Long orderNo;

    /**
     * 用户位置 - 纬度
     */
    @NotNull(message = "用户位置纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度范围必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度范围必须在-90到90之间")
    private Double latitude;

    /**
     * 用户位置 - 经度
     */
    @NotNull(message = "用户位置经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度范围必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度范围必须在-180到180之间")
    private Double longitude;
}
