package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-22:43
 * @Description: 订单统计视图
 */
@Data
@Builder
public class VenueOrderStatisticsVo {

    /**
     * 总订单数
     */
    @NonNull
    private Integer totalOrders;

    /**
     * 待确认订单数
     */
    @NonNull
    private Integer pendingOrders;

    /**
     * 已完成订单数
     */
    @NonNull
    private Integer completedOrders;

    /**
     * 已取消订单数
     */
    @NonNull
    private Integer cancelledOrders;

    /**
     * 总收入
     */
    @NonNull
    private BigDecimal totalRevenue;

    /**
     * 今日订单数
     */
    @NonNull
    private Integer todayOrders;

    /**
     * 今日收入
     */
    @NonNull
    private BigDecimal todayRevenue;
}