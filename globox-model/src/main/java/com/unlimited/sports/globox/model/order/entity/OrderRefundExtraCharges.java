package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;
import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 退款申请关联的额外费用退款明细表
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value ="order_refund_extra_charges")
@Data
@Builder
public class OrderRefundExtraCharges extends BaseEntity {
    /**
     * 订单号
     */
    private Long orderNo;

    /**
     * 退款申请ID(order_refund_apply.id)
     */
    private Long refundApplyId;

    /**
     * 额外费用ID(order_extra_charges.id)
     */
    private Long extraChargeId;

    /**
     * 订单项ID(可选)，订单级额外费用为空
     */
    private Long orderItemId;

    /**
     * 该额外费用实际退款金额（快照）
     */
    private BigDecimal refundAmount;

}