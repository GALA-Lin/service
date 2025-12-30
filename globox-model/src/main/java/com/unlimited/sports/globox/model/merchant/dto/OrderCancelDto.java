package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * @since 2025-12-18-22:35
 * 订单取消DTO
 */
@Data
public class OrderCancelDto {

    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /**
     * 取消类型：1=全部取消，2=部分取消
     */
    @NotNull(message = "取消类型不能为空")
    private Integer cancelType;

    /**
     * 要取消的时段ID列表（部分取消时必填）
     */
    private List<Long> slotIds;

    /**
     * 取消原因
     */
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因长度不能超过500字符")
    private String cancelReason;
}