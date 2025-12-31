package com.unlimited.sports.globox.model.order.dto;

import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import java.util.List;

/**
 * 订单分页查询请求 DTO
 */
@Data
@Schema(name = "GetOrderPageDto", description = "订单分页查询请求参数")
public class GetOrderPageDto {

    /**
     * 页码（从 1 开始）
     */
    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "页码（从1开始）",
            example = "1",
            defaultValue = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Schema(description = "每页记录数",
            example = "10",
            defaultValue = "10",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer pageSize = 10;

    /**
     * 订单状态（不传表示全部）
     */
    @Schema(description = "订单状态列表（不传或为空表示全部状态）",
            example = "PAID",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<OrderStatusEnum> orderStatus;
}