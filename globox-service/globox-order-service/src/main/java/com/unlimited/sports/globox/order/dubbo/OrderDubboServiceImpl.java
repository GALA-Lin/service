package com.unlimited.sports.globox.order.dubbo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.constants.MQConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.message.UnlockSlotMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.order.OrderDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.model.order.vo.RefundTimelineVo;
import com.unlimited.sports.globox.order.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
@DubboService(group = "dev", validation = "true")
public class OrderDubboServiceImpl implements OrderDubboService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderRefundApplyMapper orderRefundApplyMapper;

    @Autowired
    private OrderExtraChargesMapper orderExtraChargesMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private OrderItemRefundsMapper orderItemRefundsMapper;

    @Autowired
    private OrderExtraChargeLinksMapper orderExtraChargeLinksMapper;

    @Autowired
    private OrderRefundApplyItemsMapper orderRefundApplyItemsMapper;

    @Autowired
    private OrderRefundExtraChargesMapper orderRefundExtraChargesMapper;

    @Autowired
    private MQService mqService;

    @Override
    public IPage<MerchantGetOrderResultDto> merchantGetOrderPage(
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
            return empty;
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

                    return MerchantGetOrderResultDto.builder()
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
                })
                .toList();

        // 4. 构建分页返回
        Page<MerchantGetOrderResultDto> result =
                new Page<>(dto.getPageNum(), dto.getPageSize(), orderPage.getTotal());
        result.setRecords(merchantGetOrderResultDtos);

        return result;
    }

    @Override
    public MerchantGetOrderResultDto merchantGetOrderDetails(MerchantGetOrderDetailsRequestDto dto) {
        Long orderNo = dto.getOrderNo();
        Long venueId = dto.getVenueId();

        // 1. 查询订单主表
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .last("limit 1"));

        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        // 2. 查询订单项
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .orderByAsc(OrderItems::getStartTime));

        Assert.isNotEmpty(items, OrderCode.ORDER_ITEM_NOT_EXIST);

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
        return MerchantGetOrderResultDto.builder()
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantCancelOrderResultDto merchantCancelUnpaidOrder(
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
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        // 只能取消场馆订单
        Assert.isTrue(order.getSellerType() == SellerTypeEnum.VENUE,
                OrderCode.ORDER_NOT_EXIST);

        // 2) 幂等：如果已取消，直接返回成功
        if (order.getOrderStatus() == OrderStatusEnum.CANCELLED) {
            return MerchantCancelOrderResultDto.builder()
                    .orderNo(orderNo)
                    .success(true)
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getDescription())
                    .cancelledAt(order.getCancelledAt())
                    .build();
        }

        // 3) 状态校验：只能取消未支付 + PENDING
        Assert.isTrue(order.getPaymentStatus() == PaymentStatusEnum.UNPAID,
                OrderCode.ORDER_STATUS_NOT_ALLOW_CANCEL);
        Assert.isTrue(order.getOrderStatus() == OrderStatusEnum.PENDING,
                OrderCode.ORDER_STATUS_NOT_ALLOW_CANCEL);

        LocalDateTime now = LocalDateTime.now();

        // 4) 更新订单为 CANCELLED
        Orders update = new Orders();
        update.setId(order.getId());
        update.setOrderStatus(OrderStatusEnum.CANCELLED);
        update.setCancelledAt(now);

        int rows = ordersMapper.updateById(update);
        Assert.isTrue(rows == 1, OrderCode.ORDER_CANCEL_FAILED);

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

        // 保险起见，除了下订场订单代码逻辑不对，否则一定通过
        Assert.isNotEmpty(items, OrderCode.ORDER_ITEM_NOT_EXIST);

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
                        MQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                        MQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                        unlockMsg);
            }
        });

        // 7) 返回
        return MerchantCancelOrderResultDto.builder()
                .orderNo(orderNo)
                .success(true)
                .orderStatus(OrderStatusEnum.CANCELLED)
                .orderStatusName(OrderStatusEnum.CANCELLED.getDescription())
                .cancelledAt(now)
                .build();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantApproveRefundResultDto merchantApproveRefund(MerchantApproveRefundRequestDto dto) {

        Long orderNo = dto.getOrderNo();
        Long refundApplyId = dto.getRefundApplyId();
        Long venueId = dto.getVenueId();
        Long merchantId = dto.getMerchantId();
        LocalDateTime now = LocalDateTime.now();

        // 1) 锁订单（防并发取消/支付/重复审批）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);
        Assert.isTrue(order.getOrderStatus().equals(OrderStatusEnum.REFUND_APPLYING), OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        Assert.isTrue(order.getSellerType() == SellerTypeEnum.VENUE, OrderCode.ORDER_NOT_EXIST);

        // 2) 锁退款申请（必须属于该订单）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        // 3) 幂等：已同意直接返回
        if (apply.getApplyStatus() == ApplyRefundStatusEnum.APPROVED) {
            return MerchantApproveRefundResultDto.builder()
                    .orderNo(orderNo)
                    .refundApplyId(refundApplyId)
                    .applyStatus(apply.getApplyStatus())
                    .reviewedAt(apply.getReviewedAt())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getDescription())
                    .approvedItemCount(0)
                    .build();
        }

        // 只允许 PENDING -> APPROVED
        Assert.isTrue(apply.getApplyStatus() == ApplyRefundStatusEnum.PENDING,
                OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);

        // 4) 取出本次申请的 itemId（用 apply_items 精确绑定）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));
        Assert.isNotEmpty(applyItems, OrderCode.ORDER_ITEM_NOT_EXIST);

        List<Long> applyItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Assert.isTrue(!applyItemIds.isEmpty(), OrderCode.ORDER_ITEM_NOT_EXIST);

        // 5) 查订单项并校验：必须属于该订单，且状态只能是 WAIT_APPROVING 或 APPROVED（幂等/重试安全）
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .orderByAsc(OrderItems::getId));
        Assert.isTrue(items.size() == applyItemIds.size(), OrderCode.ORDER_ITEM_NOT_EXIST);

        for (OrderItems it : items) {
            RefundStatusEnum st = it.getRefundStatus();
            Assert.isNotEmpty(st, OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
            Assert.isTrue(st == RefundStatusEnum.WAIT_APPROVING || st == RefundStatusEnum.APPROVED,
                    OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
        }

        // 6) 判断“本次审批后是否整单退款”
        //    规则：除本次 applyItemIds 之外，订单内不允许存在 NONE/WAIT_APPROVING（其它申请）这类“未纳入退款闭环”的 item
        Long blockingCount = orderItemsMapper.selectCount(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .notIn(OrderItems::getId, applyItemIds)
                        .in(OrderItems::getRefundStatus,
                                RefundStatusEnum.NONE,
                                RefundStatusEnum.WAIT_APPROVING));
        boolean fullRefundAfterThis = (blockingCount == null || blockingCount == 0);

        // 7) 写入“订单项级额外费用退款明细”（order_refund_extra_charges）
        //    依据：order_extra_charge_links.allocated_amount（退款以此为准）
        //    注意：只写本次申请涉及 item 的相关 extra；订单级 extra 仅在整单时写入
        List<OrderExtraChargeLinks> links = orderExtraChargeLinksMapper.selectList(
                Wrappers.<OrderExtraChargeLinks>lambdaQuery()
                        .eq(OrderExtraChargeLinks::getOrderNo, orderNo)
                        .in(OrderExtraChargeLinks::getOrderItemId, applyItemIds));

        Map<Long, OrderExtraCharges> chargeMap = new HashMap<>();
        if (links != null && !links.isEmpty()) {
            List<Long> chargeIds = links.stream()
                    .map(OrderExtraChargeLinks::getExtraChargeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!chargeIds.isEmpty()) {
                chargeMap = orderExtraChargesMapper.selectBatchIds(chargeIds).stream()
                        .collect(Collectors.toMap(OrderExtraCharges::getId, c -> c));
            }
        }

        // itemId -> 本次批准涉及的 item 级 extra 合计（用于回填 order_item_refunds.extra_charge_amount）
        Map<Long, BigDecimal> itemExtraSum = new HashMap<>();

        if (links != null) {
            for (OrderExtraChargeLinks lk : links) {
                OrderExtraCharges ch = chargeMap.get(lk.getExtraChargeId());
                if (ch == null) continue;

                // item 级额外费用（order_item_id != null）
                if (ch.getOrderItemId() == null) continue;

                BigDecimal amt = lk.getAllocatedAmount() == null ? BigDecimal.ZERO : lk.getAllocatedAmount();

                // 幂等：同一个 refundApplyId + extraChargeId 只插一次（你有唯一约束的话可省 exists）
                boolean already = orderRefundExtraChargesMapper.exists(
                        Wrappers.<OrderRefundExtraCharges>lambdaQuery()
                                .eq(OrderRefundExtraCharges::getRefundApplyId, refundApplyId)
                                .eq(OrderRefundExtraCharges::getExtraChargeId, lk.getExtraChargeId())
                );
                if (!already) {
                    OrderRefundExtraCharges rec = OrderRefundExtraCharges.builder()
                            .orderNo(orderNo)
                            .refundApplyId(refundApplyId)
                            .extraChargeId(lk.getExtraChargeId())
                            .orderItemId(lk.getOrderItemId())
                            .refundAmount(amt)
                            .build();
                    orderRefundExtraChargesMapper.insert(rec);
                }

                itemExtraSum.merge(lk.getOrderItemId(), amt, BigDecimal::add);
            }
        }

        // 8) 整单退款时：记录订单级额外费用（order_item_id IS NULL）
        if (fullRefundAfterThis) {
            List<OrderExtraCharges> orderLevelCharges = orderExtraChargesMapper.selectList(
                    Wrappers.<OrderExtraCharges>lambdaQuery()
                            .eq(OrderExtraCharges::getOrderNo, orderNo)
                            .isNull(OrderExtraCharges::getOrderItemId));

            for (OrderExtraCharges ch : orderLevelCharges) {
                boolean already = orderRefundExtraChargesMapper.exists(
                        Wrappers.<OrderRefundExtraCharges>lambdaQuery()
                                .eq(OrderRefundExtraCharges::getRefundApplyId, refundApplyId)
                                .eq(OrderRefundExtraCharges::getExtraChargeId, ch.getId()));
                if (!already) {
                    OrderRefundExtraCharges rec = OrderRefundExtraCharges.builder()
                            .orderNo(orderNo)
                            .refundApplyId(refundApplyId)
                            .extraChargeId(ch.getId())
                            .orderItemId(null)
                            .refundAmount(ch.getChargeAmount())
                            .build();
                    orderRefundExtraChargesMapper.insert(rec);
                }
            }
        }

        // 9) 生成 item 退款事实（order_item_refunds） + item 状态流转 + item级日志
        int approvedCount = 0;
        for (OrderItems it : items) {

            // 9.1 退款事实：不存在则插入（幂等）
            boolean factExists = orderItemRefundsMapper.exists(
                    Wrappers.<OrderItemRefunds>lambdaQuery()
                            .eq(OrderItemRefunds::getOrderItemId, it.getId()));

            if (!factExists) {
                // 基础金额：按你当前策略用 unit_price（不含额外费用）
                BigDecimal itemAmount = it.getUnitPrice();
                Assert.isNotEmpty(itemAmount, OrderCode.ORDER_ITEM_NOT_EXIST);

                BigDecimal extraChargeAmount = itemExtraSum.getOrDefault(it.getId(), BigDecimal.ZERO);
                BigDecimal refundFee = BigDecimal.ZERO; // TODO 手续费策略
                BigDecimal refundAmount = itemAmount.add(extraChargeAmount).subtract(refundFee);

                OrderItemRefunds fact = OrderItemRefunds.builder()
                        .orderNo(orderNo)
                        .orderItemId(it.getId())
                        .refundApplyId(refundApplyId)
                        .itemAmount(itemAmount)
                        .extraChargeAmount(extraChargeAmount)
                        .refundFee(refundFee)
                        .refundAmount(refundAmount)
                        .refundStatus(RefundStatusEnum.APPROVED)
                        .build();
                orderItemRefundsMapper.insert(fact);
            }

            // 9.2 仅对 WAIT_APPROVING 的做状态迁移；已经 APPROVED 的直接跳过（幂等）
            if (it.getRefundStatus() == RefundStatusEnum.WAIT_APPROVING) {
                int ur = orderItemsMapper.update(
                        null,
                        Wrappers.<OrderItems>lambdaUpdate()
                                .eq(OrderItems::getId, it.getId())
                                .eq(OrderItems::getOrderNo, orderNo)
                                .eq(OrderItems::getRefundStatus, RefundStatusEnum.WAIT_APPROVING)
                                .set(OrderItems::getRefundStatus, RefundStatusEnum.APPROVED));
                Assert.isTrue(ur == 1, OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);

                approvedCount++;

                // 9.3 item 级日志：只在“真正迁移成功”后插入
                OrderStatusLogs itemLog = OrderStatusLogs.builder()
                        .orderNo(orderNo)
                        .orderId(order.getId())
                        .orderItemId(it.getId())
                        .action(OrderActionEnum.REFUND_APPROVE)
                        .oldOrderStatus(order.getOrderStatus())
                        .newOrderStatus(OrderStatusEnum.REFUNDING)
                        .oldItemRefundStatus(RefundStatusEnum.WAIT_APPROVING)
                        .newItemRefundStatus(RefundStatusEnum.APPROVED)
                        .refundApplyId(refundApplyId)
                        .operatorType(OperatorTypeEnum.MERCHANT)
                        .operatorId(merchantId)
                        .operatorName("MERCHANT_" + merchantId)
                        .remark(dto.getRemark() == null ? "商家同意退款申请" : dto.getRemark())
                        .build();
                orderStatusLogsMapper.insert(itemLog);
            }
        }

        // 10) 更新退款申请状态：PENDING -> APPROVED
        int ua = orderRefundApplyMapper.update(
                null,
                Wrappers.<OrderRefundApply>lambdaUpdate()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.PENDING)
                        .set(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.APPROVED)
                        .set(OrderRefundApply::getReviewedAt, now)
                        .set(OrderRefundApply::getSellerRemark, dto.getRemark()));
        Assert.isTrue(ua == 1, OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);

        // 11) 订单状态：保持 REFUND_APPLYING（或你未来加 REFUNDING 再调整）
        Orders updOrder = new Orders();
        updOrder.setId(order.getId());
        updOrder.setOrderStatus(OrderStatusEnum.REFUNDING);
        updOrder.setRefundApplyId(refundApplyId);
        ordersMapper.updateById(updOrder);

        // 订单级日志
        OrderStatusLogs orderLog = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.REFUND_APPROVE)
                .oldOrderStatus(order.getOrderStatus())
                .newOrderStatus(OrderStatusEnum.REFUNDING)
                .refundApplyId(refundApplyId)
                .operatorType(OperatorTypeEnum.MERCHANT)
                .operatorId(merchantId)
                .operatorName("MERCHANT_" + merchantId)
                .remark(dto.getRemark() == null ? "商家同意退款申请" : dto.getRemark())
                .build();
        orderStatusLogsMapper.insert(orderLog);

//        List<Long> slotIds = items.stream().map(OrderItems::getSlotId).toList();
//        LocalDate bookingDate = items.get(0).getBookingDate();
//
//        // 事务提交后发送通知到商家服务，解锁槽
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @Override
//            public void afterCommit() {
//                UnlockSlotMessage unlockMsg = UnlockSlotMessage.builder()
//                        .userId(order.getBuyerId())
//                        .slotIds(slotIds)
//                        .bookingDate(bookingDate)
//                        .build();
//
//                mqService.send(
//                        MQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
//                        MQConstants.ROUTING_ORDER_UNLOCK_SLOT,
//                        unlockMsg);
//            }
//        });

        // TODO ETA 2026/01/03 afterCommit：发 MQ 给支付服务发起退款 / 通知用户

        return MerchantApproveRefundResultDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.APPROVED)
                .reviewedAt(now)
                .orderStatus(OrderStatusEnum.REFUNDING)
                .orderStatusName(OrderStatusEnum.REFUNDING.getDescription())
                .approvedItemCount(approvedCount)
                .build();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantRejectRefundResultDto merchantRejectRefund(MerchantRejectRefundRequestDto dto) {

        Long orderNo = dto.getOrderNo();
        Long refundApplyId = dto.getRefundApplyId();
        Long venueId = dto.getVenueId();
        Long merchantId = dto.getMerchantId();
        LocalDateTime now = LocalDateTime.now();

        // 1) 锁订单（防并发取消/支付/重复审批）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);
        Assert.isTrue(order.getSellerType() == SellerTypeEnum.VENUE, OrderCode.ORDER_NOT_EXIST);
        Assert.isTrue(order.getOrderStatus().equals(OrderStatusEnum.REFUND_APPLYING), OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        // 2) 锁退款申请（必须属于该订单）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        // 3) 幂等：已拒绝直接返回
        if (apply.getApplyStatus() == ApplyRefundStatusEnum.REJECTED) {
            return MerchantRejectRefundResultDto.builder()
                    .orderNo(orderNo)
                    .refundApplyId(refundApplyId)
                    .applyStatus(ApplyRefundStatusEnum.REJECTED)
                    .reviewedAt(apply.getReviewedAt())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getDescription())
                    .rejectedItemCount(0)
                    .build();
        }

        // 只允许从 PENDING -> REJECTED（已 APPROVED 就不允许拒绝了）
        Assert.isTrue(apply.getApplyStatus() == ApplyRefundStatusEnum.PENDING,
                OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);

        // 4) 取出本次申请涉及的 itemId（精准绑定）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));
        Assert.isNotEmpty(applyItems, OrderCode.ORDER_ITEM_NOT_EXIST);

        List<Long> applyItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Assert.isTrue(!applyItemIds.isEmpty(), OrderCode.ORDER_ITEM_NOT_EXIST);

        // 5) 查询订单项并校验必须处于 WAIT_APPROVING（否则就是串单/并发/重复处理）
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .orderByAsc(OrderItems::getId));
        Assert.isTrue(items.size() == applyItemIds.size(), OrderCode.ORDER_ITEM_NOT_EXIST);

        for (OrderItems it : items) {
            Assert.isTrue(it.getRefundStatus() == RefundStatusEnum.WAIT_APPROVING,
                    OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
        }

        // 6) 更新申请状态：PENDING -> REJECTED
        int ua = orderRefundApplyMapper.update(
                null,
                Wrappers.<OrderRefundApply>lambdaUpdate()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.PENDING)
                        .set(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.REJECTED)
                        .set(OrderRefundApply::getReviewedAt, now)
                        .set(OrderRefundApply::getSellerRemark, dto.getRemark()));
        Assert.isTrue(ua == 1, OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);

        // 7) 更新订单项：WAIT_APPROVING -> NONE（只更新本次申请的 itemId 集合）
        //    你说“只能全部拒绝”，所以这里必须全部更新成功
        int updatedCnt = orderItemsMapper.update(
                null,
                Wrappers.<OrderItems>lambdaUpdate()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .eq(OrderItems::getRefundStatus, RefundStatusEnum.WAIT_APPROVING)
                        .set(OrderItems::getRefundStatus, RefundStatusEnum.NONE));
        Assert.isTrue(updatedCnt == applyItemIds.size(), OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);

        // 8) 订单状态：直接置为 REFUND_REJECTED
        Orders updOrder = new Orders();
        updOrder.setId(order.getId());
        updOrder.setOrderStatus(OrderStatusEnum.REFUND_REJECTED);
        updOrder.setRefundApplyId(refundApplyId);
        ordersMapper.updateById(updOrder);

        // 9) 写 item 级日志（审批拒绝后再插入）
        for (OrderItems it : items) {
            OrderStatusLogs itemLog = OrderStatusLogs.builder()
                    .orderNo(orderNo)
                    .orderId(order.getId())
                    .orderItemId(it.getId())
                    .action(OrderActionEnum.REFUND_REJECT)
                    .oldOrderStatus(order.getOrderStatus())
                    .newOrderStatus(OrderStatusEnum.REFUND_REJECTED)
                    .oldItemRefundStatus(RefundStatusEnum.WAIT_APPROVING)
                    .newItemRefundStatus(RefundStatusEnum.NONE)
                    .refundApplyId(refundApplyId)
                    .operatorType(OperatorTypeEnum.MERCHANT)
                    .operatorId(merchantId)
                    .operatorName("MERCHANT_" + merchantId)
                    .remark(dto.getRemark() == null ? "商家拒绝退款申请" : dto.getRemark())
                    .build();
            orderStatusLogsMapper.insert(itemLog);
        }

        // 10) 写订单级日志
        OrderStatusLogs orderLog = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.REFUND_REJECT)
                .oldOrderStatus(order.getOrderStatus())
                .newOrderStatus(OrderStatusEnum.REFUND_REJECTED)
                .refundApplyId(refundApplyId)
                .operatorType(OperatorTypeEnum.MERCHANT)
                .operatorId(merchantId)
                .operatorName("MERCHANT_" + merchantId)
                .remark(dto.getRemark() == null ? "商家拒绝退款申请" : dto.getRemark())
                .build();
        orderStatusLogsMapper.insert(orderLog);

        return MerchantRejectRefundResultDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.REJECTED)
                .reviewedAt(now)
                .orderStatus(OrderStatusEnum.REFUND_REJECTED)
                .orderStatusName(OrderStatusEnum.REFUND_REJECTED.getDescription())
                .rejectedItemCount(applyItemIds.size())
                .build();
    }

    @Override
    public IPage<MerchantRefundApplyPageResultDto> merchantGetRefundApplyPage(
                        MerchantRefundApplyPageRequestDto dto) {

        Page<MerchantRefundApplyPageResultDto> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        // 1) 先分页查“申请单主信息”（必须 join orders 才能按 venueIds 过滤，避免丢页）
        IPage<MerchantRefundApplyPageResultDto> resPage =
                orderRefundApplyMapper.selectMerchantRefundApplyPage(page, dto);

        List<MerchantRefundApplyPageResultDto> records = resPage.getRecords();
        if (records == null || records.isEmpty()) {
            return resPage;
        }

        // 2) 批量准备 key 集合
        List<Long> refundApplyIds = records.stream()
                .map(MerchantRefundApplyPageResultDto::getRefundApplyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> orderNos = records.stream()
                .map(MerchantRefundApplyPageResultDto::getOrderNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 3) 申请项明细：applyId -> itemIds + count
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .in(OrderRefundApplyItems::getRefundApplyId, refundApplyIds));

        Map<Long, List<Long>> applyIdToItemIds = applyItems.stream()
                .collect(Collectors.groupingBy(
                        OrderRefundApplyItems::getRefundApplyId,
                        Collectors.mapping(OrderRefundApplyItems::getOrderItemId, Collectors.toList())));

        Map<Long, Integer> applyIdToItemCount = new HashMap<>();
        applyIdToItemIds.forEach((k, v) -> applyIdToItemCount.put(k, v == null ? 0 : (int) v.stream().distinct().count()));

        // 4) 拉取本页涉及的所有 item（用于计算 item 金额）
        List<Long> allItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, OrderItems> itemMap = allItemIds.isEmpty()
                ? Map.of()
                : orderItemsMapper.selectBatchIds(allItemIds).stream()
                .collect(Collectors.toMap(OrderItems::getId, it -> it, (a, b) -> a));

        // 5) 计算 item 级额外费用（非 SLOT extra）：按 links.allocated_amount，且只认 “订单项级费用”（charge.order_item_id != null）
        Map<Long, BigDecimal> itemIdToExtraSum = new HashMap<>();
        if (!allItemIds.isEmpty()) {
            List<OrderExtraChargeLinks> links = orderExtraChargeLinksMapper.selectList(
                    Wrappers.<OrderExtraChargeLinks>lambdaQuery()
                            .in(OrderExtraChargeLinks::getOrderItemId, allItemIds));

            if (links != null && !links.isEmpty()) {
                List<Long> chargeIds = links.stream()
                        .map(OrderExtraChargeLinks::getExtraChargeId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                Map<Long, OrderExtraCharges> chargeMap = chargeIds.isEmpty()
                        ? Map.of()
                        : orderExtraChargesMapper.selectBatchIds(chargeIds).stream()
                        .collect(Collectors.toMap(OrderExtraCharges::getId, ch -> ch, (a, b) -> a));

                for (OrderExtraChargeLinks lk : links) {
                    OrderExtraCharges ch = chargeMap.get(lk.getExtraChargeId());
                    if (ch == null) continue;

                    // 只统计 “订单项级额外费用”（order_extra_charges.order_item_id != null）
                    if (ch.getOrderItemId() == null) continue;

                    BigDecimal amt = lk.getAllocatedAmount() == null ? BigDecimal.ZERO : lk.getAllocatedAmount();
                    itemIdToExtraSum.merge(lk.getOrderItemId(), amt, BigDecimal::add);
                }
            }
        }

        // 6) 判断“是否整单申请”：applyItemCount == 订单 item 总数
        //    （仅用于展示 totalRefundAmount 时把订单级 extra 加进去；实际是否退订单级 extra 你在审批环节控制）
        Map<Long, Long> orderNoToTotalItemCount = new HashMap<>();
        if (!orderNos.isEmpty()) {
            // select order_no, count(1) as cnt from order_items where order_no in (...) and deleted=0 group by order_no
            List<Map<String, Object>> maps = orderItemsMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderItems>()
                            .select("order_no", "COUNT(1) AS cnt")
                            .in("order_no", orderNos)
                            .eq("deleted", 0)
                            .groupBy("order_no"));
            for (Map<String, Object> m : maps) {
                Long ono = ((Number) m.get("order_no")).longValue();
                Long cnt = ((Number) m.get("cnt")).longValue();
                orderNoToTotalItemCount.put(ono, cnt);
            }
        }

        // 7) 订单级额外费用：sum(order_extra_charges.charge_amount where order_item_id is null)
        Map<Long, BigDecimal> orderNoToOrderLevelExtraSum = new HashMap<>();
        if (!orderNos.isEmpty()) {
            List<Map<String, Object>> maps = orderExtraChargesMapper.selectMaps(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<OrderExtraCharges>()
                            .select("order_no", "COALESCE(SUM(charge_amount),0) AS amt")
                            .in("order_no", orderNos)
                            .isNull("order_item_id")
                            .eq("deleted", 0)
                            .groupBy("order_no"));
            for (Map<String, Object> m : maps) {
                Long ono = ((Number) m.get("order_no")).longValue();
                BigDecimal amt = (m.get("amt") == null) ? BigDecimal.ZERO : new BigDecimal(String.valueOf(m.get("amt")));
                orderNoToOrderLevelExtraSum.put(ono, amt);
            }
        }

        // 8) 回填每条记录：applyItemCount / totalRefundAmount
        for (MerchantRefundApplyPageResultDto r : records) {
            Long applyId = r.getRefundApplyId();
            Long ono = r.getOrderNo();

            List<Long> itemIds = applyIdToItemIds.getOrDefault(applyId, List.of()).stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            int cnt = applyIdToItemCount.getOrDefault(applyId, 0);
            r.setApplyItemCount(cnt);

            // item 金额口径：unit_price + extra_amount（record extra），不要依赖 subtotal
            BigDecimal itemSum = BigDecimal.ZERO;
            BigDecimal itemExtraSum = BigDecimal.ZERO;

            for (Long itemId : itemIds) {
                OrderItems it = itemMap.get(itemId);
                if (it == null) continue;

                BigDecimal unit = it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice();
                BigDecimal recordExtra = it.getExtraAmount() == null ? BigDecimal.ZERO : it.getExtraAmount();

                itemSum = itemSum.add(unit.add(recordExtra));
                itemExtraSum = itemExtraSum.add(itemIdToExtraSum.getOrDefault(itemId, BigDecimal.ZERO));
            }

            // 展示口径：如果本次申请覆盖了整单所有 items，则额外展示“订单级 extra”
            BigDecimal orderLevelExtra = BigDecimal.ZERO;
            Long totalItemCnt = orderNoToTotalItemCount.getOrDefault(ono, 0L);
            if (totalItemCnt != null && totalItemCnt > 0 && (long) cnt == totalItemCnt) {
                orderLevelExtra = orderNoToOrderLevelExtraSum.getOrDefault(ono, BigDecimal.ZERO);
            }

            // fee
            BigDecimal fee = BigDecimal.ZERO;

            BigDecimal totalRefund = itemSum.add(itemExtraSum).add(orderLevelExtra).subtract(fee);
            r.setTotalRefundAmount(totalRefund);
        }

        return resPage;
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantRefundApplyDetailsResultDto merchantGetRefundApplyDetails(MerchantRefundApplyDetailsRequestDto dto) {

        Long refundApplyId = dto.getRefundApplyId();
        Long orderNo = dto.getOrderNo();
        Long venueId = dto.getVenueId();

        // 1) 查订单（校验该场馆是否匹配）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, venueId)
                        .eq(Orders::getSellerType, SellerTypeEnum.VENUE));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        // 2) 查退款申请
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo));
        Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        // 3) 查本次申请包含哪些 items（apply_items）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));
        Assert.isNotEmpty(applyItems, OrderCode.ORDER_ITEM_NOT_EXIST);

        List<Long> itemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // 4) 查订单项
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, itemIds)
                        .orderByAsc(OrderItems::getId));
        Assert.isTrue(items.size() == itemIds.size(), OrderCode.ORDER_ITEM_NOT_EXIST);

        // 5) 查 item 退款事实（建议按 refund_apply_id + item_id 查当前申请的事实）
        List<OrderItemRefunds> facts = orderItemRefundsMapper.selectList(
                Wrappers.<OrderItemRefunds>lambdaQuery()
                        .eq(OrderItemRefunds::getOrderNo, orderNo)
                        .eq(OrderItemRefunds::getRefundApplyId, refundApplyId)
                        .in(OrderItemRefunds::getOrderItemId, itemIds));
        Map<Long, OrderItemRefunds> factMap = facts.stream()
                .collect(Collectors.toMap(OrderItemRefunds::getOrderItemId, f -> f, (a, b) -> a));

        // 6) 查 extraCharges（本次申请产生的额外费用退款明细）
        List<OrderRefundExtraCharges> refundExtras = orderRefundExtraChargesMapper.selectList(
                Wrappers.<OrderRefundExtraCharges>lambdaQuery()
                        .eq(OrderRefundExtraCharges::getOrderNo, orderNo)
                        .eq(OrderRefundExtraCharges::getRefundApplyId, refundApplyId)
                        .orderByAsc(OrderRefundExtraCharges::getId));

        // extra_charge_id -> charge（拿 name/typeId/order_item_id 等展示字段）
        Map<Long, OrderExtraCharges> extraChargeMap = Map.of();
        if (refundExtras != null && !refundExtras.isEmpty()) {
            List<Long> extraIds = refundExtras.stream()
                    .map(OrderRefundExtraCharges::getExtraChargeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!extraIds.isEmpty()) {
                extraChargeMap = orderExtraChargesMapper.selectBatchIds(extraIds).stream()
                        .collect(Collectors.toMap(OrderExtraCharges::getId, x -> x, (a, b) -> a));
            }
        }

        // itemId -> item级extra合计（用来聚合展示与金额计算）
        Map<Long, BigDecimal> itemExtraSum = new HashMap<>();
        BigDecimal orderLevelExtraSum = BigDecimal.ZERO;

        List<MerchantRefundExtraChargeDto> extraChargeDtos = new ArrayList<>();
        if (refundExtras != null) {
            for (OrderRefundExtraCharges re : refundExtras) {
                BigDecimal amt = re.getRefundAmount() == null ? BigDecimal.ZERO : re.getRefundAmount();

                if (re.getOrderItemId() != null) {
                    itemExtraSum.merge(re.getOrderItemId(), amt, BigDecimal::add);
                } else {
                    orderLevelExtraSum = orderLevelExtraSum.add(amt);
                }

                OrderExtraCharges ch = extraChargeMap.get(re.getExtraChargeId());
                extraChargeDtos.add(MerchantRefundExtraChargeDto.builder()
                        .extraChargeId(re.getExtraChargeId())
                        .chargeTypeId(ch == null ? null : ch.getChargeTypeId())
                        .orderItemId(re.getOrderItemId())
                        .chargeName(ch == null ? null : ch.getChargeName())
                        .refundAmount(amt)
                        .build());
            }
        }

        // 7) 组装 item 进度
        List<MerchantRefundItemDto> itemDtos = new ArrayList<>();
        BigDecimal totalRefundAmount = BigDecimal.ZERO;
        BigDecimal refundedAmount = BigDecimal.ZERO;
        BigDecimal refundingAmount = BigDecimal.ZERO;

        for (OrderItems it : items) {

            OrderItemRefunds fact = factMap.get(it.getId());
            BigDecimal itemAmount = (fact != null && fact.getItemAmount() != null)
                    ? fact.getItemAmount()
                    : (it.getUnitPrice() == null ? BigDecimal.ZERO : it.getUnitPrice());

            BigDecimal extraAmt = itemExtraSum.getOrDefault(it.getId(), BigDecimal.ZERO);

            BigDecimal fee = (fact != null && fact.getRefundFee() != null) ? fact.getRefundFee() : BigDecimal.ZERO;
            BigDecimal refundAmt = (fact != null && fact.getRefundAmount() != null)
                    ? fact.getRefundAmount()
                    : itemAmount.add(extraAmt).subtract(fee);

            totalRefundAmount = totalRefundAmount.add(refundAmt);

            // 根据 item refundStatus 粗分：已完成/处理中
            RefundStatusEnum rs = it.getRefundStatus();
            if (rs == RefundStatusEnum.COMPLETED) {
                refundedAmount = refundedAmount.add(refundAmt);
            } else if (rs == RefundStatusEnum.APPROVED || rs == RefundStatusEnum.PENDING) {
                refundingAmount = refundingAmount.add(refundAmt);
            }

            itemDtos.add(MerchantRefundItemDto.builder()
                    .orderItemId(it.getId())
                    .venueId(venueId)
                    .recordId(it.getRecordId())
                    .refundStatus(rs)
                    .itemAmount(itemAmount)
                    .extraChargeAmount(extraAmt)
                    .refundFee(fee)
                    .refundAmount(refundAmt)
                    .build());
        }

        // 订单级 extra（如果你希望展示在 total 里）：
        // 这里我把它加到 totalRefundAmount，但 refunded/refunding 是否加，看订单状态/退款时间判断
        totalRefundAmount = totalRefundAmount.add(orderLevelExtraSum);

        if (orderLevelExtraSum.compareTo(BigDecimal.ZERO) > 0) {
            if (apply.getRefundCompletedAt() != null || order.getOrderStatus() == OrderStatusEnum.REFUNDED) {
                refundedAmount = refundedAmount.add(orderLevelExtraSum);
            } else if (apply.getRefundInitiatedAt() != null) {
                refundingAmount = refundingAmount.add(orderLevelExtraSum);
            }
        }

        // 8) 时间线（可选）
        List<RefundTimelineVo> timeline = List.of();
        if (dto.isIncludeTimeline()) {
            List<OrderStatusLogs> logs = orderStatusLogsMapper.selectList(
                    Wrappers.<OrderStatusLogs>lambdaQuery()
                            .eq(OrderStatusLogs::getOrderNo, orderNo)
                            .eq(OrderStatusLogs::getRefundApplyId, refundApplyId)
                            .in(OrderStatusLogs::getAction,
                                    OrderActionEnum.REFUND_APPLY,
                                    OrderActionEnum.REFUND_APPROVE,
                                    OrderActionEnum.REFUND_REJECT,
                                    OrderActionEnum.REFUND_COMPLETE
                            )
                            .orderByAsc(OrderStatusLogs::getCreatedAt)
            );

            timeline = logs.stream().map(statusLogs -> RefundTimelineVo.builder()
                    .action(statusLogs.getAction())
                    .actionName(statusLogs.getAction().getDescription())
                    .at(statusLogs.getCreatedAt())
                    .remark(statusLogs.getRemark())
                    .operatorType(statusLogs.getOperatorType())
                    .operatorId(statusLogs.getOperatorId())
                    .operatorName(statusLogs.getOperatorName())
                    .build()).toList();
        }

        // 9) 组装返回
        return MerchantRefundApplyDetailsResultDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .userId(order.getBuyerId())
                .venueId(venueId)
                .venueName(order.getSellerName())
                .orderStatus(order.getOrderStatus())
                .applyStatus(apply.getApplyStatus())
                .reasonCode(apply.getReasonCode())
                .reasonDetail(apply.getReasonDetail())
                .appliedAt(apply.getCreatedAt())
                .reviewedAt(apply.getReviewedAt())
                .sellerRemark(apply.getSellerRemark())
                .refundInitiatedAt(apply.getRefundInitiatedAt())
                .refundCompletedAt(apply.getRefundCompletedAt())
                .refundTransactionId(apply.getRefundTransactionId())
                .totalRefundAmount(totalRefundAmount)
                .refundedAmount(refundedAmount)
                .refundingAmount(refundingAmount)
                .items(itemDtos)
                .extraCharges(extraChargeDtos)
                .timeline(timeline)
                .build();
    }

    /**
     * 是否能通过商户取消
     *
     * @param orderStatusEnum           订单当前状态
     * @param orderItemRefundStatusEnum 订单项当前退款状态
     * @return 商户是否可取消
     */
    private boolean isOrderItemCancelableByMerchant(OrderStatusEnum orderStatusEnum, RefundStatusEnum orderItemRefundStatusEnum) {
        // 订单已完成 / 已取消 / 已退款 则 不可取消
        if (orderStatusEnum == OrderStatusEnum.COMPLETED
                || orderStatusEnum == OrderStatusEnum.CANCELLED
                || orderStatusEnum == OrderStatusEnum.REFUNDED) {
            return false;
        }

        // 订单项已进入退款流程 则 不可取消
        return orderItemRefundStatusEnum == RefundStatusEnum.NONE;
    }
}
