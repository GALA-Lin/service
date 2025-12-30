package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import com.unlimited.sports.globox.model.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单项退款表（部分退款的事实表）
 */
@Data
@Builder
@TableName(value ="order_item_refunds")
@EqualsAndHashCode(callSuper = true)
public class OrderItemRefunds extends BaseEntity implements Serializable {

    /**
     * 订单号
     */
    private Long orderNo;

    /**
     * 订单明细ID
     */
    private Long orderItemId;

    /**
     * 关联的退款申请ID
     */
    private Long refundApplyId;

    /**
     * 该订单项原始金额（含自身slot extra）
     */
    private BigDecimal itemAmount;

    /**
     * 该订单项关联的额外费用金额
     */
    private BigDecimal extraChargeAmount;

    /**
     * 手续费（平台扣除）
     */
    private BigDecimal refundFee;

    /**
     * 实际退款金额（扣除手续费后）
     */
    private BigDecimal refundAmount;

    /**
     * 退款状态：1=PENDING，2=APPROVED，3=COMPLETED
     */
    private RefundStatusEnum refundStatus;




    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}