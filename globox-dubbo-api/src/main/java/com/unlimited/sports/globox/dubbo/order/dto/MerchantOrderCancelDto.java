package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class MerchantOrderCancelDto {

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
     * 要取消的时段ID列表
     */
    @NotNull
    @Size(message = "订单槽不能为空")
    private List<Long> slotIds;

    /**
     * 取消原因
     */
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因长度不能超过500字符")
    private String cancelReason;
}