package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.model.merchant.enums.BookingSlotStatusEnum;
import com.unlimited.sports.globox.model.merchant.enums.VenueOrderPayStatusEnum;
import com.unlimited.sports.globox.model.merchant.enums.VenueOrderStatusEnum;
import com.unlimited.sports.globox.merchant.mapper.BookingSlotMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueOrderMapper;
import com.unlimited.sports.globox.merchant.service.RefundService;
import com.unlimited.sports.globox.merchant.service.VenueOrderService;
import com.unlimited.sports.globox.model.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.vo.*;
import com.unlimited.sports.globox.model.merchant.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-23:01
 * @Description:
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueOrderServiceImpl implements VenueOrderService {

    private final VenueOrderMapper venueOrderMapper;
    private final BookingSlotMapper bookingSlotMapper;
    private final VenueMapper venueMapper;
    private final CourtMapper courtMapper;  // 添加 CourtMapper
    private final RefundService refundService;

    @Override
    public IPage<VenueOrderVo> queryMerchantOrders(Long merchantId, OrderQueryDto queryDTO) {
        // 构建分页对象
        Page<VenueOrder> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 查询订单列表
        IPage<VenueOrder> orderPage = venueOrderMapper.selectMerchantOrderPage(
                page,
                merchantId,
                queryDTO.getVenueId(),
                queryDTO.getOrderStatus(),
                queryDTO.getPaymentStatus(),
                queryDTO.getStartTime(),
                queryDTO.getEndTime(),
                queryDTO.getOrderNo()
        );

        // 批量查询所有订单的时段
        List<Long> orderIds = orderPage.getRecords().stream()
                .map(VenueOrder::getOrderId)
                .collect(Collectors.toList());

        // 批量查询所有时段
        Map<Long, List<VenueBookingSlot>> orderSlotsMap;
        if (!orderIds.isEmpty()) {
            List<VenueBookingSlot> allSlots = bookingSlotMapper.selectByOrderIds(orderIds);
            orderSlotsMap = allSlots.stream()
                    .collect(Collectors.groupingBy(VenueBookingSlot::getOrderId));

            // 批量查询所有场地信息
            Set<Long> courtIds = allSlots.stream()
                    .map(VenueBookingSlot::getCourtId)
                    .collect(Collectors.toSet());

            Map<Long, Court> courtMap = Collections.emptyMap();
            if (!courtIds.isEmpty()) {
                List<Court> courts = courtMapper.selectByIds(new ArrayList<>(courtIds));
                courtMap = courts.stream()
                        .collect(Collectors.toMap(Court::getCourtId, court -> court));
            }

            // 缓存场地映射供后续使用
            Map<Long, Court> finalCourtMap = courtMap;

            // 转换为VO并填充slots
            Map<Long, List<VenueBookingSlot>> finalOrderSlotsMap = orderSlotsMap;
            return orderPage.convert(order -> {
                VenueOrderVo vo = convertToOrderVO(order);

                List<VenueBookingSlot> slots = finalOrderSlotsMap.get(order.getOrderId());
                if (slots != null && !slots.isEmpty()) {
                    List<VenueBookingSlotVo> slotVos = slots.stream()
                            .map(slot -> convertToSlotVO(slot, finalCourtMap))
                            .collect(Collectors.toList());
                    vo.setSlots(slotVos);
                } else {
                    vo.setSlots(new ArrayList<>());
                }

                return vo;
            });
        }

        return orderPage.convert(this::convertToOrderVO);
    }

    //TODO 与认证模块调试，UID查询用户名等，用于订单详情 ETA 2026/01/15
    @Override
    public VenueOrderVo getOrderDetail(Long merchantId, Long orderId) {
        // 查询订单
        VenueOrder order = venueOrderMapper.selectById(orderId);
        if (order == null) {
            throw new GloboxApplicationException("订单不存在");
        }

        // 验证订单归属
        Venue venue = venueMapper.selectById(order.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权访问该订单");
        }

        // 查询订单时段
        List<VenueBookingSlot> slots = bookingSlotMapper.selectByOrderId(orderId);

        // 批量查询场地信息
        Map<Long, Court> courtMap = Collections.emptyMap();
        if (!slots.isEmpty()) {
            Set<Long> courtIds = slots.stream()
                    .map(VenueBookingSlot::getCourtId)
                    .collect(Collectors.toSet());

            if (!courtIds.isEmpty()) {
                List<Court> courts = courtMapper.selectByIds(new ArrayList<>(courtIds));
                courtMap = courts.stream()
                        .collect(Collectors.toMap(Court::getCourtId, court -> court));
            }
        }

        // 转换为VO
        VenueOrderVo venueOrderVO = convertToOrderVO(order);

        Map<Long, Court> finalCourtMap = courtMap;
        List<VenueBookingSlotVo> slotVos = slots.stream()
                .map(slot -> convertToSlotVO(slot, finalCourtMap))
                .collect(Collectors.toList());
        venueOrderVO.setSlots(slotVos);

        return venueOrderVO;
    }

    /**
     * 取消订单
     * @param merchantId 商家ID
     * @param cancelDTO  取消订单条件
     * @return 取消后的订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderCancelResultVo cancelOrder(Long merchantId, OrderCancelDto cancelDTO) {
        // 验证订单
        VenueOrder order = venueOrderMapper.selectById(cancelDTO.getOrderId());
        if (order == null) {
            throw new GloboxApplicationException("订单不存在");
        }

        Venue venue = venueMapper.selectById(order.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该订单");
        }

        if (VenueOrderStatusEnum.CANCELLED.getCode().equals(order.getOrderStatus())) {
            throw new GloboxApplicationException("订单已取消,无需重复操作");
        }

        if (VenueOrderStatusEnum.COMPLETED.getCode().equals(order.getOrderStatus())) {
            throw new GloboxApplicationException("订单已完成,无法取消");
        }

        List<VenueBookingSlot> allSlots = bookingSlotMapper.selectByOrderId(order.getOrderId());

        // 执行取消逻辑
        OrderCancelResultVo result;
        if (cancelDTO.getCancelType() == 1) {
            // 全部取消
            result = cancelAllSlots(order, allSlots, cancelDTO.getCancelReason());
        } else if (cancelDTO.getCancelType() == 2) {
            // 部分取消
            if (cancelDTO.getSlotIds() == null || cancelDTO.getSlotIds().isEmpty()) {
                throw new GloboxApplicationException("部分取消时必须指定要取消的时段");
            }
            result = cancelPartialSlots(order, allSlots, cancelDTO.getSlotIds(),
                    cancelDTO.getCancelReason());
        } else {
            throw new GloboxApplicationException("无效的取消类型");
        }

        log.info("订单取消成功,订单号:{},取消类型:{},退款金额:{}",
                order.getOrderNo(), cancelDTO.getCancelType(), result.getRefundAmount());

        return result;
    }

    /**
     * 全部取消
     * 业务逻辑:
     * 1. 先调用退款服务
     * 2. 退款成功后才释放时段
     * 3. 更新订单状态
     * 4. 如果退款失败,抛出异常,事务回滚
     */
    private OrderCancelResultVo cancelAllSlots(VenueOrder order, List<VenueBookingSlot> slots,
                                               String cancelReason) {
        // 调用退款服务
        RefundRequestDto refundRequest = RefundRequestDto.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .refundType(1)
                .refundAmount(order.getTotalPrice())
                .refundReason(cancelReason)
                .userId(order.getUserId())
                .venueId(order.getVenueId())
                .build();

        RefundResultVo refundResult;
        try {
            refundResult = refundService.processRefund(refundRequest);
        } catch (Exception e) {
            log.error("退款服务调用异常,订单号:{}", order.getOrderNo(), e);
            throw new GloboxApplicationException("退款服务异常,取消订单失败: " + e.getMessage());
        }

        // 检查退款是否成功
        if (!refundResult.getSuccess()) {
            log.error("全部退款失败,订单号:{},错误:{}",
                    order.getOrderNo(), refundResult.getErrorMessage());
            throw new GloboxApplicationException("退款失败: " + refundResult.getErrorMessage());
        }

        log.info("全部退款成功,订单号:{},退款金额:{},退款单号:{}",
                order.getOrderNo(), order.getTotalPrice(), refundResult.getRefundNo());

        // 退款成功后才释放所有时段
        List<Long> cancelledSlotIds = new ArrayList<>();
        for (VenueBookingSlot slot : slots) {
            slot.setStatus(BookingSlotStatusEnum.BOOKABLE.getCode());
            slot.setOrderId(null);
            bookingSlotMapper.updateById(slot);
            cancelledSlotIds.add(slot.getBookingSlotId());
        }
        log.info("已释放 {} 个时段,订单号:{}", slots.size(), order.getOrderNo());

        // 更新订单状态为已取消
        order.setOrderStatus(VenueOrderStatusEnum.CANCELLED.getCode());
        venueOrderMapper.updateById(order);

        // 构建返回结果
        VenueOrderStatusEnum orderStatus = VenueOrderStatusEnum.getByCode(order.getOrderStatus());

        return OrderCancelResultVo.builder()
                .success(true)
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .cancelType(1)
                .cancelledSlotCount(slots.size())
                .remainingSlotCount(0)
                .refundAmount(order.getTotalPrice())
                .refundNo(refundResult.getRefundNo())
                .orderStatus(order.getOrderStatus())
                .orderStatusName(orderStatus != null ? orderStatus.getName() : "未知")
                .remainingAmount(BigDecimal.ZERO)
                .cancelReason(cancelReason)
                .cancelledSlotIds(cancelledSlotIds)
                .cancelledAt(LocalDateTime.now())
                .message("订单全部取消成功,已退款" + order.getTotalPrice() + "元")
                .refundDetail(OrderCancelResultVo.RefundDetailVo.builder()
                        .refundStatus(2) // 成功
                        .refundStatusName("退款成功")
                        .refundAmount(order.getTotalPrice())
                        .refundNo(refundResult.getRefundNo())
                        .refundTime(LocalDateTime.now())
                        .estimatedArrivalTime("1-3个工作日")
                        .refundRemark("订单已全部取消,款项将原路退回")
                        .build())
                .build();
    }

    /**
     * 部分取消 - 返回详细结果
     */
    private OrderCancelResultVo cancelPartialSlots(VenueOrder order, List<VenueBookingSlot> allSlots,
                                                   List<Long> cancelSlotIds, String cancelReason) {
        // 第一步:验证要取消的时段
        List<Long> validSlotIds = allSlots.stream()
                .map(VenueBookingSlot::getBookingSlotId)
                .toList();

        for (Long slotId : cancelSlotIds) {
            if (!validSlotIds.contains(slotId)) {
                throw new GloboxApplicationException("时段ID:" + slotId + " 不属于该订单");
            }
        }

        // 第二步:计算退款金额
        BigDecimal refundAmount = BigDecimal.ZERO;
        List<VenueBookingSlot> slotsToCancel = new ArrayList<>();

        for (VenueBookingSlot slot : allSlots) {
            if (cancelSlotIds.contains(slot.getBookingSlotId())) {
                slotsToCancel.add(slot);
                refundAmount = refundAmount.add(slot.getUnitPrice());
            }
        }

        // 第三步:调用退款服务
        RefundRequestDto refundRequest = RefundRequestDto.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .refundType(2)
                .refundAmount(refundAmount)
                .refundReason(cancelReason)
                .cancelledSlotIds(cancelSlotIds)
                .userId(order.getUserId())
                .venueId(order.getVenueId())
                .build();

        RefundResultVo refundResult;
        try {
            refundResult = refundService.processRefund(refundRequest);
        } catch (Exception e) {
            log.error("退款服务调用异常,订单号:{}", order.getOrderNo(), e);
            throw new GloboxApplicationException("退款服务异常,取消订单失败: " + e.getMessage());
        }

        // 第四步:检查退款是否成功
        if (!refundResult.getSuccess()) {
            log.error("部分退款失败,订单号:{},错误:{}",
                    order.getOrderNo(), refundResult.getErrorMessage());
            throw new GloboxApplicationException("退款失败: " + refundResult.getErrorMessage());
        }

        log.info("部分退款成功,订单号:{},退款金额:{},退款单号:{}",
                order.getOrderNo(), refundAmount, refundResult.getRefundNo());

        // 第五步:退款成功后才释放时段
        for (VenueBookingSlot slot : slotsToCancel) {
            slot.setStatus(BookingSlotStatusEnum.BOOKABLE.getCode());
            slot.setOrderId(null);
            bookingSlotMapper.updateById(slot);
        }
        log.info("已释放 {} 个时段,订单号:{}", slotsToCancel.size(), order.getOrderNo());

        // 第六步:检查是否全部取消
        long remainingSlots = allSlots.stream()
                .filter(s -> !cancelSlotIds.contains(s.getBookingSlotId()))
                .count();

        if (remainingSlots == 0) {
            order.setOrderStatus(VenueOrderStatusEnum.CANCELLED.getCode());
        }

        // 第七步:更新订单金额
        BigDecimal newTotalPrice = order.getTotalPrice().subtract(refundAmount);
        order.setTotalPrice(newTotalPrice);
        venueOrderMapper.updateById(order);

        // 构建返回结果
        VenueOrderStatusEnum orderStatus = VenueOrderStatusEnum.getByCode(order.getOrderStatus());

        return OrderCancelResultVo.builder()
                .success(true)
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .cancelType(2)
                .cancelledSlotCount(slotsToCancel.size())
                .remainingSlotCount((int) remainingSlots)
                .refundAmount(refundAmount)
                .refundNo(refundResult.getRefundNo())
                .orderStatus(order.getOrderStatus())
                .orderStatusName(orderStatus != null ? orderStatus.getName() : "未知")
                .remainingAmount(newTotalPrice)
                .cancelReason(cancelReason)
                .cancelledSlotIds(cancelSlotIds)
                .cancelledAt(LocalDateTime.now())
                .message(String.format("订单部分取消成功,已取消%d个时段,退款%.2f元,剩余%d个时段",
                        slotsToCancel.size(), refundAmount, remainingSlots))
                .refundDetail(OrderCancelResultVo.RefundDetailVo.builder()
                        .refundStatus(2)
                        .refundStatusName("退款成功")
                        .refundAmount(refundAmount)
                        .refundNo(refundResult.getRefundNo())
                        .refundTime(LocalDateTime.now())
                        .estimatedArrivalTime("1-3个工作日")
                        .refundRemark(String.format("已取消%d个时段,款项将原路退回", slotsToCancel.size()))
                        .build())
                .build();
    }



    @Override
    @Transactional(rollbackFor = Exception.class)
    public VenueOrderVo confirmOrder(Long merchantId, Long orderId) {
        // 查询订单
        VenueOrder order = venueOrderMapper.selectById(orderId);
        if (order == null) {
            throw new GloboxApplicationException("订单不存在");
        }

        // 验证订单归属
        Venue venue = venueMapper.selectById(order.getVenueId());
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该订单");
        }

        // 验证订单状态
        if (!VenueOrderStatusEnum.PENDING.getCode().equals(order.getOrderStatus())) {
            throw new GloboxApplicationException("订单状态不是待确认，无法确认");
        }

        // 更新订单状态
        order.setOrderStatus(VenueOrderStatusEnum.CONFIRMED.getCode());
        venueOrderMapper.updateById(order);

        log.info("订单确认成功，订单号：{}", order.getOrderNo());

        // 返回确认后的完整订单信息
        return getOrderDetail(merchantId, orderId);
    }

    @Override
    public VenueOrderStatisticsVo getOrderStatistics(Long merchantId, Long venueId) {
        // 统计各状态订单数量
        Integer totalOrders = venueOrderMapper.countMerchantOrders(merchantId, null, null);
        Integer pendingOrders = venueOrderMapper.countMerchantOrders(
                merchantId, VenueOrderStatusEnum.PENDING.getCode(), null);
        Integer completedOrders = venueOrderMapper.countMerchantOrders(
                merchantId, VenueOrderStatusEnum.COMPLETED.getCode(), null);
        Integer cancelledOrders = venueOrderMapper.countMerchantOrders(
                merchantId, VenueOrderStatusEnum.CANCELLED.getCode(), null);

        // 使用 Builder 构建 VO
        return VenueOrderStatisticsVo.builder()
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(BigDecimal.ZERO)
                .todayOrders(0)
                .todayRevenue(BigDecimal.ZERO)
                .build();
    }

    /**
     * 转换为OrderVO
     */
    private VenueOrderVo convertToOrderVO(VenueOrder order) {
        // 获取状态名称
        VenueOrderStatusEnum orderStatus = VenueOrderStatusEnum.getByCode(order.getOrderStatus());
        VenueOrderPayStatusEnum paymentStatus = VenueOrderPayStatusEnum.getByCode(order.getPaymentStatus());

        // 使用 Builder 构建 VO
        return VenueOrderVo.builder()
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .venueId(order.getVenueId())
                .basePrice(order.getBasePrice())
                .extraChargeTotal(order.getExtraChargeTotal())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalPrice(order.getTotalPrice())
                .paymentStatus(order.getPaymentStatus())
                .paymentStatusName(paymentStatus != null ? paymentStatus.getName() : "未知")
                .orderStatus(order.getOrderStatus())
                .orderStatusName(orderStatus != null ? orderStatus.getName() : "未知")
                .source(order.getSource())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .slots(new ArrayList<>()) // 初始化空列表
                .build();
    }

    /**
     * 转换为BookingSlotVO - 带场地信息映射
     */
    private VenueBookingSlotVo convertToSlotVO(VenueBookingSlot slot, Map<Long, Court> courtMap) {
        // 获取状态名称
        BookingSlotStatusEnum status = BookingSlotStatusEnum.getByCode(slot.getStatus());

        // 获取场地名称
        String courtName = "";
        if (courtMap != null) {
            Court court = courtMap.get(slot.getCourtId());
            if (court != null) {
                courtName = court.getName();
            }
        }

        // 使用 Builder 构建 VO
        return VenueBookingSlotVo.builder()
                .bookingSlotId(slot.getBookingSlotId())
                .courtId(slot.getCourtId())
                .courtName(courtName)
                .bookingDate(slot.getBookingDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .unitPrice(slot.getUnitPrice())
                .status(slot.getStatus())
                .statusName(status != null ? status.getName() : "未知")
                .cancelable(BookingSlotStatusEnum.OCCUPIED.getCode().equals(slot.getStatus()))
                .build();
    }
}