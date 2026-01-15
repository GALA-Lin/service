package com.unlimited.sports.globox.model.order.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.model.base.BaseEntity;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;


/**
 * 订单主表
 */
@TableName(value ="orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Orders extends BaseEntity implements Serializable {

    /**
     * 业务订单号
     */
    private Long orderNo;

    /**
     * 对外（支付平台）订单号
     */
    private String outTradeNo;

    /**
     * 第三方交易编号
     */
    private String tradeNo;

    /**
     * VENUE 订单场地归属：1=home平台，2=away平台
     */
    private Integer sourcePlatform;

    /**
     * 购买方用户 ID
     */
    private Long buyerId;

    /**
     * 提供方名称（快照）
     */
    private String sellerName;

    /**
     * 服务提供方: VENUE / COACH
     */
    private SellerTypeEnum sellerType;

    /**
     * 提供方 ID（商家/教练）
     */
    private Long sellerId;

    /**
     * 订单状态：1=PENDING，2=CONFIRMED，3=COMPLETED，4=CANCELLED
     */
    private OrderStatusEnum orderStatus;

    /**
     * 支付状态：1=UNPAID，2=PAID，3=REFUNDING，4=REFUNDED
     */
    private OrdersPaymentStatusEnum paymentStatus;

    /**
     * 最新的退款申请ID（用于快速查询退款状态）
     */
    private Long refundApplyId;

    /**
     * 基础价格（所有时段价格之和）
     */
    private BigDecimal baseAmount;

    /**
     * 额外费用总和
     */
    private BigDecimal extraAmount;

    /**
     * 小计（=base_price + extra_amount）
     */
    private BigDecimal subtotal;

    /**
     * 实际需要支付的金额（=subtotal - discount_amount）
     */
    private BigDecimal payAmount;

    private PaymentTypeEnum paymentType;

    /**
     * 是否活动订单
     */
    private Boolean activity;

    /**
     * 折扣金额（后续业务拓展）
     */
    private BigDecimal discountAmount;

    /**
     * 支付时间
     */
    private LocalDateTime paidAt;

    /**
     * 取消时间
     */
    private LocalDateTime cancelledAt;

    /**
     * 备注
     */
    private String remark;

    /**
     * 完成时间
     */
    private LocalDate completedAt;


    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}