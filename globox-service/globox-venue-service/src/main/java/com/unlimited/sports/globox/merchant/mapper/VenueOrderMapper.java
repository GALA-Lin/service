package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.model.merchant.entity.VenueOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-23:06
 * @Description:
 */
@Mapper
public interface VenueOrderMapper extends BaseMapper<VenueOrder> {
    /**
     * 分页查询商家的订单列表
     * @param page 分页对象
     * @param merchantId 商家ID
     * @param venueId 场馆ID
     * @param orderStatus 订单状态
     * @param paymentStatus 支付状态
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param orderNo 订单号
     * @return
     */
    IPage<VenueOrder> selectMerchantOrderPage(
            Page<?> page,
            @Param("merchantId") Long merchantId,
            @Param("venueId") Long venueId,
            @Param("orderStatus") Integer orderStatus,
            @Param("paymentStatus") Integer paymentStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("orderNo") String orderNo
    );

    /**
     * 统计商家订单数量
     */
    Integer countMerchantOrders(
            @Param("merchantId") Long merchantId,
            @Param("orderStatus") Integer orderStatus,
            @Param("paymentStatus") Integer paymentStatus
    );

    /**
     * 查询订单详情（包含关联信息）
     */
    VenueOrder selectOrderDetail(@Param("orderId") Long orderId);
}
