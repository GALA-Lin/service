package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-22:40
 * @Description: 订单视图
 */
@Data
@Builder
public class VenueOrderVo {

    /**
     * 订单ID
     */
    @NonNull
    private Long orderId;

    /**
     * 订单号
     */
    @NonNull
    private String orderNo;

    /**
     * 用户ID
     */
    @NonNull
    private Long userId;

    /**
     * 用户昵称
     */
    private String userNickname;

    /**
     * 场馆ID
     */
    @NonNull
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 基础价格
     */
    @NonNull
    private BigDecimal basePrice;

    /**
     * 额外费用总和
     */
    @NonNull
    private BigDecimal extraChargeTotal;

    /**
     * 小计
     */
    @NonNull
    private BigDecimal subtotal;

    /**
     * 折扣金额
     */
    @NonNull
    private BigDecimal discountAmount;

    /**
     * 最终总价
     */
    @NonNull
    private BigDecimal totalPrice;

    /**
     * 支付状态
     */
    @NonNull
    private Integer paymentStatus;

    /**
     * 支付状态名称
     */
    private String paymentStatusName;

    /**
     * 订单状态
     */
    @NonNull
    private Integer orderStatus;

    /**
     * 订单状态名称
     */
    private String orderStatusName;

    /**
     * 订单来源
     */
    @NonNull
    private Integer source;

    /**
     * 支付时间
     */
    @NonNull
    private LocalDateTime paidAt;

    /**
     * 创建时间
     */
    @NonNull
    private LocalDateTime createdAt;

    /**
     * 订单时段列表
     */
    @NonNull
    private List<VenueBookingSlotVo> slots;
}
