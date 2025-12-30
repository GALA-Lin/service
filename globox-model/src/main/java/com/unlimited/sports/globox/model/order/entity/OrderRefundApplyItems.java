package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.model.base.BaseEntity;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 退款申请-订单项明细表（记录本次申请包含哪些订单项）
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value ="order_refund_apply_items")
@Data
@Builder
public class OrderRefundApplyItems extends BaseEntity {
    /**
     * 退款申请ID（order_refund_apply.id）
     */
    private Long refundApplyId;

    /**
     * 订单号（冗余，便于按订单查询）
     */
    private Long orderNo;

    /**
     * 订单明细ID（order_items.id）
     */
    private Long orderItemId;
}