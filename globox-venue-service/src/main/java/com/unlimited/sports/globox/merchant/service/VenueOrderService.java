package com.unlimited.sports.globox.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.model.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.vo.*;


/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-22:54
 * @Description:
 */
public interface VenueOrderService {

    /**
     * 分页查询商家订单列表
     * @param merchantId 商家ID
     * @param queryDTO 查询条件
     * @return 分页订单列表
     */
    IPage<VenueOrderVo> queryMerchantOrders(Long merchantId, OrderQueryDto queryDTO);

    /**
     * 查询订单详情
     * @param merchantId 商家ID
     * @param orderId 订单ID
     * @return 订单详情
     */
    VenueOrderVo getOrderDetail(Long merchantId, Long orderId);

    /**
     * (全部/部分)取消订单
     *
     * @param merchantId 商家ID
     * @param cancelDTO  取消订单条件
     * @return
     */
    OrderCancelResultVo cancelOrder(Long merchantId, OrderCancelDto cancelDTO);

    /**
     * 确认订单
     *
     * @param merchantId 商家ID
     * @param orderId    订单ID
     * @return
     */
    VenueOrderVo confirmOrder(Long merchantId, Long orderId);

    /**
     * 获取订单统计数据
     * @param merchantId  商家ID
     * @param venueId  场馆ID
     * @return 订单统计数据
     */
    VenueOrderStatisticsVo getOrderStatistics(Long merchantId, Long venueId);
}