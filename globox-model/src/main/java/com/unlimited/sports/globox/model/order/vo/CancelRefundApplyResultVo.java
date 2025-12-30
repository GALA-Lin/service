package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 代表取消退款申请后的结果值对象。
 * 包含了订单编号、退款申请ID、申请单状态及其名称、订单状态及其名称、取消时间以及可选的提示信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelRefundApplyResultVo implements Serializable {

    @NotNull
    private Long orderNo;
    @NotNull
    private Long refundApplyId;

    /**
     * 申请单状态
     */
    @NotNull
    private ApplyRefundStatusEnum applyStatus;
    @NotNull
    private String applyStatusName;

    /**
     * 订单状态（取消退款后订单状态可能需要回退/或置为某个状态）
     */
    @NotNull
    private OrderStatusEnum orderStatus;
    @NotNull
    private String orderStatusName;

    /**
     * 取消时间
     */
    @Null
    private LocalDateTime cancelledAt;
}