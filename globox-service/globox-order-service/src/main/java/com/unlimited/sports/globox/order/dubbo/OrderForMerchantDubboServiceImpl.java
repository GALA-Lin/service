package com.unlimited.sports.globox.order.dubbo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.message.order.OrderNotifyMerchantConfirmMessage;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.model.order.vo.RefundTimelineVo;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.lock.RedisLock;
import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.OrderRefundService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 订单 dubbo 服务
 */
@Slf4j
@Component
@DubboService(group = "rpc")
public class OrderForMerchantDubboServiceImpl implements OrderForMerchantDubboService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private MQService mqService;

    @Autowired
    private OrderActivitiesMapper orderActivitiesMapper;

    /**
     * 商家分页查询用户订单信息。
     *
     * @param dto 商家分页查询订单请求参数，包含商家ID、页码和每页大小
     * @return 返回分页后的订单信息列表，每个订单信息包括订单号、用户ID、场馆信息、价格明细、支付状态、订单状态等
     */
    @Override
    public RpcResult<IPage<MerchantGetOrderResultDto>> getOrderPage(
            MerchantGetOrderPageRequestDto dto) {
        // 1. 分页查询订单主表
        Page<Orders> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        IPage<Orders> orderPage = ordersMapper.selectPage(
                page,
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getSellerId, dto.getVenueId())
                        .orderByDesc(Orders::getId));

        if (orderPage.getRecords().isEmpty()) {
            Page<MerchantGetOrderResultDto> empty =
                    new Page<>(dto.getPageNum(), dto.getPageSize(), 0);
            empty.setRecords(Collections.emptyList());
            return RpcResult.ok(empty);
        }

        // 2. 批量查询订单项
        List<Long> orderNos = orderPage.getRecords()
                .stream()
                .map(Orders::getOrderNo)
                .toList();

        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .in(OrderItems::getOrderNo, orderNos)
                        .orderByAsc(OrderItems::getStartTime));

        Map<Long, List<OrderItems>> itemMap = items.stream()
                .collect(Collectors.groupingBy(OrderItems::getOrderNo));

        // 3. 组装返回 DTO
        List<MerchantGetOrderResultDto> merchantGetOrderResultDtos = orderPage.getRecords()
                .stream()
                .map(order -> {

                    List<OrderItems> orderItems =
                            itemMap.getOrDefault(order.getOrderNo(), Collections.emptyList());

                    List<RecordDto> recordDtos = orderItems.stream()
                            .map(item -> {

                                boolean cancelable = isOrderItemCancelableByMerchant(
                                        order.getOrderStatus(), item.getRefundStatus());

                                return RecordDto.builder()
                                        .recordId(item.getRecordId())
                                        .orderNo(item.getOrderNo())
                                        .courtId(item.getResourceId())
                                        .courtName(item.getResourceName())
                                        .bookingDate(item.getBookingDate())
                                        .startTime(item.getStartTime())
                                        .endTime(item.getEndTime())
                                        .unitPrice(item.getUnitPrice())
                                        .status(item.getRefundStatus())
                                        .statusName(item.getRefundStatus().getDescription())
                                        .cancelable(cancelable)
                                        .build();
                            })
                            .toList();

                    MerchantGetOrderResultDto resultDto = MerchantGetOrderResultDto.builder()
                            .orderNo(order.getOrderNo())
                            .userId(order.getBuyerId())
                            .venueId(order.getSellerId())
                            .venueName(order.getSellerName())
                            .basePrice(order.getBaseAmount())
                            .extraChargeTotal(order.getExtraAmount())
                            .subtotal(order.getSubtotal())
                            .discountAmount(order.getDiscountAmount())
                            .totalPrice(order.getPayAmount())
                            .paymentStatus(order.getPaymentStatus())
                            .paymentStatusName(order.getPaymentStatus().getDescription())
                            .orderStatus(order.getOrderStatus())
                            .orderStatusName(order.getOrderStatus().getDescription())
                            .source(order.getSourcePlatform())
                            .paidAt(order.getPaidAt())
                            .createdAt(order.getCreatedAt())
                            .records(recordDtos)
                            .build();

                    OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                            Wrappers.<OrderActivities>lambdaQuery()
                                    .eq(OrderActivities::getOrderNo, order.getOrderNo()));

                    if (!ObjectUtils.isEmpty(orderActivities)) {
                        resultDto.setActivity(true);
                        resultDto.setActivityTypeName(orderActivities.getActivityTypeName());
                    } else {
                        resultDto.setActivity(false);
                    }

                    return resultDto;
                })
                .toList();

        // 4. 构建分页返回
        Page<MerchantGetOrderResultDto> result =
                new Page<>(dto.getPageNum(), dto.getPageSize(), orderPage.getTotal());
        result.setRecords(merchantGetOrderResultDtos);

        return RpcResult.ok(result);
    }

    @Override
    public RpcResult<MerchantGetOrderResultDto> getOrderDetails(MerchantGetOrderDetailsRequestDto dto) {
        Long orderNo = dto.getOrderNo();
        Long venueId = dto.getVenueId();

        // 1. 查询订单主表
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .last("limit 1"));

        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        // 2. 查询订单项
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .orderByAsc(OrderItems::getStartTime));
        if (ObjectUtils.isEmpty(items)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        // 3. 构建 recordDtos
        List<RecordDto> recordDtos = items.stream()
                .map(item -> {
                    boolean cancelable = switch (order.getOrderStatus()) {
                        case CONFIRMED, COMPLETED, CANCELLED, REFUNDED -> false;
                        default -> true;
                    };

                    if (item.getRefundStatus() != RefundStatusEnum.NONE) {
                        cancelable = false;
                    }

                    return RecordDto.builder()
                            .recordId(item.getRecordId())
                            .orderNo(item.getOrderNo())
                            .courtId(item.getResourceId())
                            .courtName(item.getResourceName())
                            .bookingDate(item.getBookingDate())
                            .startTime(item.getStartTime())
                            .endTime(item.getEndTime())
                            .unitPrice(item.getUnitPrice())
                            .status(item.getRefundStatus())
                            .statusName(item.getRefundStatus().name())
                            .cancelable(cancelable)
                            .build();
                })
                .toList();

        // 4. 构建返回 DTO
        MerchantGetOrderResultDto resultDto = MerchantGetOrderResultDto.builder()
                .orderNo(order.getOrderNo())
                .userId(order.getBuyerId())
                .venueId(order.getSellerId())
                .venueName(order.getSellerName())
                .basePrice(order.getBaseAmount())
                .extraChargeTotal(order.getExtraAmount())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .totalPrice(order.getPayAmount())
                .paymentStatus(order.getPaymentStatus())
                .paymentStatusName(order.getPaymentStatus().getDescription())
                .orderStatus(order.getOrderStatus())
                .orderStatusName(order.getOrderStatus().getDescription())
                .source(order.getSourcePlatform())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .records(recordDtos)
                .build();


        OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                Wrappers.<OrderActivities>lambdaQuery()
                        .eq(OrderActivities::getOrderNo, order.getOrderNo()));

        if (!ObjectUtils.isEmpty(orderActivities)) {
            resultDto.setActivity(true);
            resultDto.setActivityTypeName(orderActivities.getActivityTypeName());
        } else {
            resultDto.setActivity(false);
        }

        return RpcResult.ok(resultDto);
    }


    /**
     * 商家取消未支付订单。
     *
     * @param dto 包含订单号、商家ID以及可选的取消原因的请求参数
     * @return 取消订单的结果，包括订单号、当前订单状态、状态描述及取消时间
     */
    @Override
    @RedisLock(value = "#dto.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<MerchantCancelOrderResultDto> cancelUnpaidOrder(
            MerchantCancelOrderRequestDto dto) {

        Long orderNo = dto.getOrderNo();
        Long merchantId = dto.getMerchantId();
        Long venueId = dto.getVenueId();

        // 1) 查询订单（行锁，防并发支付/取消）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .last("FOR UPDATE"));

        // 订单必须存在
        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        // 只能取消场馆订单
        if (order.getSellerType() != SellerTypeEnum.VENUE) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        // 2) 幂等：如果已取消，直接返回成功
        if (order.getOrderStatus() == OrderStatusEnum.CANCELLED) {
            MerchantCancelOrderResultDto resultDto = MerchantCancelOrderResultDto.builder()
                    .orderNo(orderNo)
                    .success(true)
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getDescription())
                    .cancelledAt(order.getCancelledAt())
                    .build();
            return RpcResult.ok(resultDto);
        }

        // 3) 状态校验：只能取消未支付 + PENDING
        if (order.getPaymentStatus() != OrdersPaymentStatusEnum.UNPAID) {
            return RpcResult.error(OrderCode.ORDER_STATUS_NOT_ALLOW_CANCEL);
        }

        if (order.getOrderStatus() != OrderStatusEnum.PENDING) {
            return RpcResult.error(OrderCode.ORDER_STATUS_NOT_ALLOW_CANCEL);
        }

        LocalDateTime now = LocalDateTime.now();

        // 4) 更新订单为 CANCELLED
        Orders update = new Orders();
        update.setId(order.getId());
        update.setOrderStatus(OrderStatusEnum.CANCELLED);
        update.setCancelledAt(now);

        int rows = ordersMapper.updateById(update);
        if (rows <= 0) {
            return RpcResult.error(OrderCode.ORDER_CANCEL_FAILED);
        }

        // 5) 写状态流转日志（订单级）
        OrderStatusLogs logEntity = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.CANCEL)
                .oldOrderStatus(order.getOrderStatus())
                .newOrderStatus(OrderStatusEnum.CANCELLED)
                .operatorType(OperatorTypeEnum.MERCHANT)
                .operatorId(merchantId)
                .operatorName("MERCHANT_" + merchantId)
                .remark("商家取消未支付订单")
                .build();
        orderStatusLogsMapper.insert(logEntity);

        // 6) 事务提交后发送“取消锁场”消息
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .orderByAsc(OrderItems::getId));

        // 保险起见，因为除了下订场订单代码逻辑不对，否则一定通过
        if (ObjectUtils.isEmpty(items)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        List<Long> recordIds = items.stream()
                .map(OrderItems::getRecordId)
                .filter(java.util.Objects::nonNull)
                .toList();

        LocalDate bookingDate = items.get(0).getBookingDate();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                UnlockSlotMessage unlockMsg = UnlockSlotMessage.builder()
                        .userId(order.getBuyerId())
                        .recordIds(recordIds)
                        .bookingDate(bookingDate)
                        .build();

                mqService.send(
                        OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                        OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                        unlockMsg);
            }
        });

        // 7) 返回
        MerchantCancelOrderResultDto resultDto = MerchantCancelOrderResultDto.builder()
                .orderNo(orderNo)
                .success(true)
                .orderStatus(OrderStatusEnum.CANCELLED)
                .orderStatusName(OrderStatusEnum.CANCELLED.getDescription())
                .cancelledAt(now)
                .build();

        return RpcResult.ok(resultDto);
    }

    /**
     * 商家确认订单的方法。
     *
     * @param dto 包含订单号的请求参数
     * @return 返回商家确认订单的结果，包括订单号、是否确认成功、当前订单状态、状态描述以及确认时间
     */
    @Override
    @RedisLock(value = "#dto.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<MerchantConfirmResultDto> confirm(MerchantConfirmRequestDto dto) {
        Long orderNo = dto.getOrderNo();
        LocalDateTime now = LocalDateTime.now();

        // 1) 查询订单
        Orders orders = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo));

        if (ObjectUtils.isEmpty(orders)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        OrderStatusEnum currentOrderStatus = orders.getOrderStatus();
        OrderStatusEnum newOrderStatus = OrderStatusEnum.CONFIRMED;

        // 是否已经被确认
        if (!orders.isConfirmed()) {
            // 2) 只有 已支付、退款被拒绝、退款已取消、部分退款成功 才可以确认订单
            if (currentOrderStatus == OrderStatusEnum.PAID
                    || currentOrderStatus == OrderStatusEnum.REFUND_REJECTED
                    || currentOrderStatus == OrderStatusEnum.REFUND_CANCELLED
                    || currentOrderStatus == OrderStatusEnum.PARTIALLY_REFUNDED) {
                int updated = ordersMapper.update(
                        null,
                        Wrappers.<Orders>lambdaUpdate()
                                .eq(Orders::getOrderNo, orderNo)
                                .eq(Orders::isConfirmed, false)
                                .in(Orders::getOrderStatus,
                                        OrderStatusEnum.PAID,
                                        OrderStatusEnum.REFUND_REJECTED,
                                        OrderStatusEnum.REFUND_CANCELLED,
                                        OrderStatusEnum.PARTIALLY_REFUNDED)
                                .set(Orders::getOrderStatus, OrderStatusEnum.CONFIRMED)
                                .set(Orders::getConfirmedAt, now)
                                .set(Orders::isConfirmed, true));
                if (updated > 0) {
                    // 3) 记录订单日志表
                    OrderStatusLogs statusLogs = OrderStatusLogs.builder()
                            .orderId(orders.getId())
                            .orderNo(orders.getOrderNo())
                            .action(OrderActionEnum.CONFIRM)
                            .oldOrderStatus(currentOrderStatus)
                            .newOrderStatus(newOrderStatus)
                            .operatorType(dto.isAutoConfirm() ? OperatorTypeEnum.SYSTEM : OperatorTypeEnum.MERCHANT)
                            .operatorName(dto.isAutoConfirm() ?
                                    OperatorTypeEnum.SYSTEM.getOperatorTypeName() : "MERCHANT_" + dto.getMerchantId())
                            .operatorId(dto.isAutoConfirm() ? null : dto.getMerchantId())
                            .remark("订单服务提供方已被确认")
                            .build();

                    orderStatusLogsMapper.insert(statusLogs);

                    MerchantConfirmResultDto resultDto = MerchantConfirmResultDto.builder()
                            .orderNo(orderNo)
                            .orderStatus(newOrderStatus)
                            .confirmAt(now)
                            .success(true)
                            .orderStatusName(newOrderStatus.getDescription())
                            .build();
                    return RpcResult.ok(resultDto);
                }
            }
        }

        MerchantConfirmResultDto resultDto = MerchantConfirmResultDto.builder()
                .orderNo(orderNo)
                .orderStatus(currentOrderStatus)
                .confirmAt(null)
                .success(false)
                .orderStatusName(currentOrderStatus.getDescription())
                .build();
        return RpcResult.ok(resultDto);
    }


    /**
     * 是否能通过商户取消/退款
     *
     * @param orderStatusEnum           订单当前状态
     * @param orderItemRefundStatusEnum 订单项当前退款状态
     * @return 商户是否可取消/退款
     */
    private boolean isOrderItemCancelableByMerchant(OrderStatusEnum orderStatusEnum, RefundStatusEnum orderItemRefundStatusEnum) {
        // 订单已完成 / 已取消 / 已退款 则 不可取消
        if (orderStatusEnum == OrderStatusEnum.REFUNDING
                || orderStatusEnum == OrderStatusEnum.CANCELLED
                || orderStatusEnum == OrderStatusEnum.REFUND_APPLYING
                || orderStatusEnum == OrderStatusEnum.REFUNDED) {
            return false;
        }

        // 订单项已进入退款流程 则 不可取消
        return orderItemRefundStatusEnum == RefundStatusEnum.NONE;
    }
}
