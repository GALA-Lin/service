package com.unlimited.sports.globox.model.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

/**
 * 获取订单详情 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "GetOrderDetailsDto", description = "获取订单详情请求参数（包含用户位置，用于距离计算）")
public class GetOrderDetailsDto {

    /**
     * 订单号
     * 可空：以路径参数为准
     */
    @Schema(description = "订单号（可选，通常以路径参数为准）",
            example = "202512180001",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    /**
     * 用户位置 - 纬度
     */
    @DecimalMin(value = "-90.0", message = "纬度范围必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度范围必须在-90到90之间")
    @Schema(description = "用户当前位置纬度",
            example = "31.230416",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Double latitude;

    /**
     * 用户位置 - 经度
     */
    @DecimalMin(value = "-180.0", message = "经度范围必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度范围必须在-180到180之间")
    @Schema(description = "用户当前位置经度",
            example = "121.473701",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Double longitude;
}
