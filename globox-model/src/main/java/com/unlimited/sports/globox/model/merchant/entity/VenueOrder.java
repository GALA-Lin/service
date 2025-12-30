package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @since  2025-12-18-11:27
 * 订单实体类
 */
@Data
@TableName("venue_orders")
public class VenueOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;

    /**
     * 订单号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 基础价格（所有时段价格之和）
     */
    @TableField("base_price")
    private BigDecimal basePrice;

    /**
     * 额外费用总和
     */
    @TableField("extra_charge_total")
    private BigDecimal extraChargeTotal;

    /**
     * 小计（=base_price + extra_charge_total）
     */
    @TableField("subtotal")
    private BigDecimal subtotal;

    /**
     * 折扣金额
     */
    @TableField("discount_amount")
    private BigDecimal discountAmount;

    /**
     * 最终总价（=subtotal - discount_amount）
     */
    @TableField("total_price")
    private BigDecimal totalPrice;

    /**
     * 支付状态：1=UNPAID，2=PAID，3=REFUNDED
     */
    @TableField("payment_status")
    private Integer paymentStatus;

    /**
     * 订单状态：1=PENDING，2=CONFIRMED，3=CANCELLED，4=COMPLETED
     */
    @TableField("order_status")
    private Integer orderStatus;

    /**
     * 订单来源：1=home平台，2=away平台
     */
    @TableField("source")
    private Integer source;

    /**
     * 支付时间
     */
    @TableField("paid_at")
    private LocalDateTime paidAt;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}