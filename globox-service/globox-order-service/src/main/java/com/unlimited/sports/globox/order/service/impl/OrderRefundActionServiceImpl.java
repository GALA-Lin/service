package com.unlimited.sports.globox.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.message.order.UserRefundMessage;
import com.unlimited.sports.globox.common.message.payment.PaymentRefundMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.lock.RedisLock;
import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.OrderRefundActionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 订单退款实际动作执行
 */
@Slf4j
@Service
public class OrderRefundActionServiceImpl implements OrderRefundActionService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderItemRefundsMapper orderItemRefundsMapper;

    @Autowired
    private OrderExtraChargesMapper orderExtraChargesMapper;

    @Autowired
    private OrderRefundApplyMapper orderRefundApplyMapper;

    @Autowired
    private OrderRefundApplyItemsMapper orderRefundApplyItemsMapper;

    @Autowired
    private OrderRefundExtraChargesMapper orderRefundExtraChargesMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private OrderExtraChargeLinksMapper orderExtraChargeLinksMapper;

    @Autowired
    private MQService mqService;


    @Autowired
    private JsonUtils jsonUtils;

    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public int refundAction(Long orderNo,
            Long refundApplyId,
            boolean isAutoRefund,
            Long merchantId) {

        // 订单项退款总基础金额
        BigDecimal totalItemAmount = BigDecimal.ZERO;
        // 订单项退款总额外费用
        BigDecimal totalItemExtra = BigDecimal.ZERO;
        // 订单退款总额外费用
        BigDecimal totalOrderLevelExtra = BigDecimal.ZERO;
        // 总手续费
        BigDecimal totalRefundFee = BigDecimal.ZERO;

        LocalDateTime now = LocalDateTime.now();

        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

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

        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds));
        Assert.isTrue(items.size() == applyItemIds.size(), OrderCode.ORDER_REFUND_ITEM_COUNT_ERROR);
        for (OrderItems it : items) {
            Assert.isTrue(it.getRefundStatus() == RefundStatusEnum.WAIT_APPROVING
                            || it.getRefundStatus() == RefundStatusEnum.APPROVED,
                    OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
        }

        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        // 如果已经被退款了，直接返回
        if (apply.getApplyStatus() == ApplyRefundStatusEnum.APPROVED) {
            return 0;
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

                // 幂等：同一个 refundApplyId + extraChargeId 只插一次
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

                    BigDecimal amt = ch.getChargeAmount() == null ? BigDecimal.ZERO : ch.getChargeAmount();
                    totalOrderLevelExtra = totalOrderLevelExtra.add(amt);
                }
            }
        }

        // 9) 生成 item 退款事实（order_item_refunds） + item 状态流转 + item级日志
        int approvedCount = 0;

        for (OrderItems it : items) {
            // 已退款的订单项跳过
            if (RefundStatusEnum.APPROVED.equals(it.getRefundStatus())) {
                continue;
            }

            // 9.1 退款事实：不存在则插入（幂等）
            boolean factExists = orderItemRefundsMapper.exists(
                    Wrappers.<OrderItemRefunds>lambdaQuery()
                            .eq(OrderItemRefunds::getRefundApplyId, refundApplyId)
                            .eq(OrderItemRefunds::getOrderItemId, it.getId()));

            if (!factExists) {
                // 基础金额：当前策略用 unit_price（不含额外费用）
                BigDecimal itemAmount = it.getUnitPrice();
                Assert.isNotEmpty(itemAmount, OrderCode.ORDER_ITEM_NOT_EXIST);

                BigDecimal extraChargeAmount = itemExtraSum.getOrDefault(it.getId(), BigDecimal.ZERO);
                // TODO 手续费策略
                BigDecimal refundFee = BigDecimal.ZERO;
                BigDecimal refundAmount = itemAmount.add(extraChargeAmount).subtract(refundFee);

                totalItemAmount = totalItemAmount.add(itemAmount);
                totalItemExtra = totalItemExtra.add(extraChargeAmount);

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
                        .operatorType(isAutoRefund ? OperatorTypeEnum.SYSTEM : OperatorTypeEnum.MERCHANT)
                        .operatorId(isAutoRefund ? null : merchantId)
                        .operatorName("MERCHANT_" + merchantId)
                        .remark(isAutoRefund ? "商家自动审批退款" : "商家同意退款")
                        .build();
                orderStatusLogsMapper.insert(itemLog);
            }
        }

        // 总退款费用
        BigDecimal totalRefundAmount = totalItemAmount
                .add(totalItemExtra)
                .add(totalOrderLevelExtra)
                .subtract(totalRefundFee);

        Assert.isTrue(totalRefundAmount.compareTo(BigDecimal.ZERO) >= 0, OrderCode.ORDER_REFUND_AMOUNT_INVALID);
        Assert.isTrue(totalRefundAmount.compareTo(order.getSubtotal()) <= 0, OrderCode.ORDER_REFUND_AMOUNT_INVALID);

        // 10) 更新退款申请状态：PENDING -> APPROVED
        String outRequestNo = UUID.randomUUID().toString().replace("-", "");
        int ua = orderRefundApplyMapper.update(
                null,
                Wrappers.<OrderRefundApply>lambdaUpdate()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.PENDING)
                        .set(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.APPROVED)
                        .set(OrderRefundApply::getOutRequestNo, outRequestNo)
                        .set(OrderRefundApply::getRefundAmount, totalRefundAmount)
                        .set(OrderRefundApply::getReviewedAt, now));
        Assert.isTrue(ua == 1, OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);

        // 11) 订单状态：REFUNDING
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
                .refundApplyId(refundApplyId)
                .operatorType(isAutoRefund ? OperatorTypeEnum.SYSTEM : OperatorTypeEnum.MERCHANT)
                .operatorId(isAutoRefund ? null : merchantId)
                .operatorName("MERCHANT_" + merchantId)
                .remark(isAutoRefund ? "商家自动审批退款" : "商家同意退款")
                .build();
        orderStatusLogsMapper.insert(orderLog);

        // 12) 发送取消锁场消息
        List<Long> recordIds = items.stream().map(OrderItems::getRecordId).toList();
        LocalDate bookingDate = items.get(0).getBookingDate();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {

                UserRefundMessage refundMessage = UserRefundMessage.builder()
                        .refundReason(apply.getReasonCode().getCode() + ":" + apply.getReasonDetail())
                        .outTradeNo(order.getOutTradeNo())
                        .outRequestNo(outRequestNo)
                        .orderNo(order.getOrderNo())
                        .refundAmount(totalRefundAmount)
                        .paymentType(order.getPaymentType())
                        .build();

                // 向支付服务发起退款
                mqService.send(
                        OrderMQConstants.EXCHANGE_TOPIC_ORDER_REFUND_APPLY_TO_PAYMENT,
                        OrderMQConstants.ROUTING_ORDER_REFUND_APPLY_TO_PAYMENT,
                        refundMessage);

                // 取消锁场
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
        return approvedCount;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundSuccess(PaymentRefundMessage message) {
        Assert.isNotEmpty(message, OrderCode.PARAM_ERROR);
        Long orderNo = message.getOrderNo();
        String outRequestNo = message.getOutRequestNo();

        Assert.isNotEmpty(orderNo, OrderCode.PARAM_ERROR);
        Assert.isTrue(outRequestNo != null && !outRequestNo.isBlank(), OrderCode.PARAM_ERROR);

        LocalDateTime now = LocalDateTime.now();

        // 1) 锁定退款申请（用 out_request_no 精确定位，FOR UPDATE 防并发）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .eq(OrderRefundApply::getOutRequestNo, outRequestNo)
                        .last("FOR UPDATE")
        );
        Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        Long refundApplyId = apply.getId();
        Assert.isNotEmpty(refundApplyId, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        // 2) 幂等：如果已记录到账时间，认为已完成过本次回调
        if (apply.getRefundCompletedAt() != null) {
            log.info("[REFUND_SUCCESS] already completed. orderNo={}, outRequestNo={}, refundApplyId={}",
                    orderNo, outRequestNo, refundApplyId);
            return;
        }

        // 3) 锁订单（保证订单状态一致）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        OrderStatusEnum oldOrderStatus = order.getOrderStatus();

        // 4) 取出本次退款申请的 item 列表（申请明细表是权威来源）
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

        Assert.isTrue(!itemIds.isEmpty(), OrderCode.ORDER_ITEM_NOT_EXIST);

        // 5) 查询这些 item 当前状态（用于写 item 级日志 / 幂等判断）
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, itemIds)
                        .last("FOR UPDATE")); // 可选：更严格一致性
        Assert.isTrue(items.size() == itemIds.size(), OrderCode.ORDER_ITEM_NOT_EXIST);

        // 6) 订单项 refund_status -> COMPLETED（仅本次申请的 items）
        orderItemsMapper.update(
                null,
                Wrappers.<OrderItems>lambdaUpdate()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, itemIds)
                        .in(OrderItems::getRefundStatus,
                                RefundStatusEnum.APPROVED, RefundStatusEnum.COMPLETED)
                        .set(OrderItems::getRefundStatus, RefundStatusEnum.COMPLETED)
        );

        // 7) 退款事实表 refund_status -> COMPLETED（仅本次 apply + items）
        orderItemRefundsMapper.update(
                null,
                Wrappers.<OrderItemRefunds>lambdaUpdate()
                        .eq(OrderItemRefunds::getOrderNo, orderNo)
                        .eq(OrderItemRefunds::getRefundApplyId, refundApplyId)
                        .in(OrderItemRefunds::getOrderItemId, itemIds)
                        .in(OrderItemRefunds::getRefundStatus,
                                RefundStatusEnum.APPROVED, RefundStatusEnum.COMPLETED)
                        .set(OrderItemRefunds::getRefundStatus, RefundStatusEnum.COMPLETED)
        );

        // 8) 更新退款申请到账信息（幂等：只在 refund_completed_at 为空时写）
        int ua = orderRefundApplyMapper.update(
                null,
                Wrappers.<OrderRefundApply>lambdaUpdate()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .isNull(OrderRefundApply::getRefundCompletedAt)
                        .set(OrderRefundApply::getRefundCompletedAt, now)
        );
        // ua==0 说明并发重复处理已写入（幂等）
        if (ua == 0) {
            log.info("[REFUND_SUCCESS] completedAt already set by others. orderNo={}, outRequestNo={}",
                    orderNo, outRequestNo);
            return;
        }

        // 9) 计算订单新状态
        long totalCount = orderItemsMapper.selectCount(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
        );

        long completedCount = orderItemsMapper.selectCount(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .eq(OrderItems::getRefundStatus, RefundStatusEnum.COMPLETED)
        );

        long processingCount = orderItemsMapper.selectCount(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getRefundStatus,
                                RefundStatusEnum.WAIT_APPROVING,
                                RefundStatusEnum.PENDING,
                                RefundStatusEnum.APPROVED)
        );

        OrderStatusEnum newOrderStatus;
        if (completedCount > 0 && completedCount == totalCount) {
            newOrderStatus = OrderStatusEnum.REFUNDED;
        } else if (processingCount > 0) {
            // 还有其它退款在路上（或本次只是部分 item 完成），保持 REFUNDING 更贴合
            newOrderStatus = OrderStatusEnum.REFUNDING;
        } else {
            // 完成了一部分，且没有其它进行中
            newOrderStatus = OrderStatusEnum.PARTIALLY_REFUNDED;
        }

        // 10) 更新订单状态（幂等：只有状态变化才写）
        if (newOrderStatus != oldOrderStatus) {
            ordersMapper.update(
                    null,
                    Wrappers.<Orders>lambdaUpdate()
                            .eq(Orders::getOrderNo, orderNo)
                            .set(Orders::getOrderStatus, newOrderStatus)
                            .set(Orders::getRefundApplyId, refundApplyId)
            );
        }

        // 11) 写订单级日志
        Map<String, Object> extra = new HashMap<>();
        extra.put("outRequestNo", outRequestNo);
        extra.put("refundApplyId", refundApplyId);
        extra.put("refundAmount", apply.getRefundAmount());

        OrderStatusLogs orderLog = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.REFUND_COMPLETE)
                .oldOrderStatus(oldOrderStatus)
                .newOrderStatus(newOrderStatus)
                .refundApplyId(refundApplyId)
                .operatorType(OperatorTypeEnum.SYSTEM)
                .operatorId(null)
                .operatorName("SYSTEM")
                .extra(jsonUtils.objectToJson(extra))
                .remark("支付退款到账回调")
                .build();
        orderStatusLogsMapper.insert(orderLog);

        // 12) 写 item 级日志（只对本次 items）
        for (OrderItems it : items) {
            RefundStatusEnum oldItemRefundStatus = it.getRefundStatus();
            if (oldItemRefundStatus == RefundStatusEnum.COMPLETED) {
                continue; // 幂等：已完成的不重复写
            }

            OrderStatusLogs itemLog = OrderStatusLogs.builder()
                    .orderNo(orderNo)
                    .orderId(order.getId())
                    .orderItemId(it.getId())
                    .action(OrderActionEnum.REFUND_COMPLETE)
                    .oldOrderStatus(oldOrderStatus)
                    .newOrderStatus(newOrderStatus)
                    .oldItemRefundStatus(oldItemRefundStatus)
                    .newItemRefundStatus(RefundStatusEnum.COMPLETED)
                    .refundApplyId(refundApplyId)
                    .operatorType(OperatorTypeEnum.SYSTEM)
                    .operatorName("SYSTEM")
                    .extra(jsonUtils.objectToJson(extra))
                    .remark("订单项退款完成")
                    .build();
            orderStatusLogsMapper.insert(itemLog);
        }

        log.info("[REFUND_SUCCESS] handled. orderNo={}, refundApplyId={}, outRequestNo={}, newOrderStatus={}",
                orderNo, refundApplyId, outRequestNo, newOrderStatus);
    }
}
