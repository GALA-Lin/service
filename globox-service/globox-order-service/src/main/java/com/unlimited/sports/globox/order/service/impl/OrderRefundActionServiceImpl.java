package com.unlimited.sports.globox.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.message.payment.PaymentRefundMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.order.util.CoachNotificationHelper;
import com.unlimited.sports.globox.order.util.VenueNotificationHelper;
import com.unlimited.sports.globox.dubbo.merchant.MerchantRefundRuleDubboService;
import com.unlimited.sports.globox.dubbo.merchant.dto.MerchantRefundRuleJudgeRequestDto;
import com.unlimited.sports.globox.dubbo.merchant.dto.MerchantRefundRuleJudgeResultVo;
import com.unlimited.sports.globox.dubbo.payment.PaymentForOrderDubboService;
import com.unlimited.sports.globox.dubbo.payment.dto.UserRefundRequestDto;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.OrderRefundActionService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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

    @Lazy
    @Autowired
    private OrderRefundActionService thisService;

    @Autowired
    private MQService mqService;

    @DubboReference(group = "rpc")
    private MerchantRefundRuleDubboService merchantRefundRuleDubboService;

    @DubboReference(group = "rpc")
    private PaymentForOrderDubboService paymentForOrderDubboService;

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private CoachNotificationHelper coachNotificationHelper;

    @Autowired
    private VenueNotificationHelper venueNotificationHelper;

    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    public int refundAction(Long orderNo,
            Long refundApplyId,
            boolean isAutoRefund,
            Long operatorId,
            OperatorTypeEnum operatorType,
            SellerTypeEnum sellerType) {

        final BigDecimal TOP = new BigDecimal("100");
        final int SCALE = 2;

        LocalDateTime now = LocalDateTime.now();

        // 1) 锁订单
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        // 2) 锁退款申请（并做幂等）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        if (apply.getApplyStatus() == ApplyRefundStatusEnum.APPROVED) {
            return 0;
        }

        // 用用户申请时间做规则判断基准，避免重试导致比例变化
        // 如果 apply.getCreatedAt() 是 Timestamp/Date，请按你的实体类型调整
        LocalDateTime refundApplyTime = apply.getCreatedAt() != null ? apply.getCreatedAt() : now;

        // 3) 查本次申请 items
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

        // 4) 判断是否整单退款（用于订单级 extra 和 payment）
        Long blockingCount = orderItemsMapper.selectCount(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .notIn(OrderItems::getId, applyItemIds)
                        .in(OrderItems::getRefundStatus,
                                RefundStatusEnum.NONE,
                                RefundStatusEnum.WAIT_APPROVING,
                                RefundStatusEnum.PENDING,
                                RefundStatusEnum.APPROVED));
        boolean fullRefundAfterThis = blockingCount == null || blockingCount == 0;

        // 5) 先计算每个 item 的退款百分比（用于 item base + item extra）
        Map<Long, BigDecimal> itemPercentMap = new HashMap<>();
        for (OrderItems it : items) {
            BigDecimal percent;

            if (sellerType == SellerTypeEnum.VENUE) {
                if (OperatorTypeEnum.MERCHANT.equals(operatorType)) {
                    // 商家发起强制 100%
                    percent = TOP;
                } else {
                    MerchantRefundRuleJudgeRequestDto requestDto = MerchantRefundRuleJudgeRequestDto.builder()
                            .venueId(order.getSellerId())
                            .orderTime(order.getCreatedAt())
                            .refundApplyTime(refundApplyTime)
                            .userId(order.getBuyerId())
                            .isActivity(order.getActivity())
                            .eventStartTime(LocalDateTime.of(it.getBookingDate(), it.getStartTime()))
                            .build();

                    RpcResult<MerchantRefundRuleJudgeResultVo> rule =
                            merchantRefundRuleDubboService.judgeApplicableRefundRule(requestDto);
                    Assert.rpcResultOk(rule);

                    MerchantRefundRuleJudgeResultVo vo = rule.getData();
                    Assert.isTrue(vo.isCanRefund(), OrderCode.ORDER_NOT_ALLOW_REFUND);

                    percent = vo.getRefundPercentage();
                }
            } else if (sellerType == SellerTypeEnum.COACH) {
                percent = TOP;
            } else {
                throw new GloboxApplicationException(OrderCode.ORDER_SELLER_TYPE_NOT_EXIST);
            }

            if (percent == null) percent = BigDecimal.ZERO;
            Assert.isTrue(percent.compareTo(BigDecimal.ZERO) >= 0 && percent.compareTo(TOP) <= 0,
                    OrderCode.ORDER_REFUND_AMOUNT_INVALID);

            itemPercentMap.put(it.getId(), percent);
        }

        // 6) item 级 extra：按 itemPercent 退款，写入 refund_extra_charges，并汇总每个 item 实退 extra
        //    links 依据 allocated_amount（退款以此为准）
        List<OrderExtraChargeLinks> links = orderExtraChargeLinksMapper.selectList(
                Wrappers.<OrderExtraChargeLinks>lambdaQuery()
                        .eq(OrderExtraChargeLinks::getOrderNo, orderNo)
                        .in(OrderExtraChargeLinks::getOrderItemId, applyItemIds));

        // itemId 实退 extra 合计（按比例）
        Map<Long, BigDecimal> itemExtraRefundSum = new HashMap<>();
        if (links != null && !links.isEmpty()) {
            for (OrderExtraChargeLinks lk : links) {
                Long itemId = lk.getOrderItemId();
                if (itemId == null) continue;

                BigDecimal origin = lk.getAllocatedAmount() == null ? BigDecimal.ZERO : lk.getAllocatedAmount();
                BigDecimal percent = itemPercentMap.getOrDefault(itemId, BigDecimal.ZERO);

                BigDecimal refundExtra = origin.multiply(percent).divide(TOP, SCALE, RoundingMode.HALF_UP);

                // 幂等插入：refundApplyId + extraChargeId 唯一
                boolean already = orderRefundExtraChargesMapper.exists(
                        Wrappers.<OrderRefundExtraCharges>lambdaQuery()
                                .eq(OrderRefundExtraCharges::getRefundApplyId, refundApplyId)
                                .eq(OrderRefundExtraCharges::getExtraChargeId, lk.getExtraChargeId()));

                if (!already) {
                    OrderRefundExtraCharges rec = OrderRefundExtraCharges.builder()
                            .orderNo(orderNo)
                            .refundApplyId(refundApplyId)
                            .extraChargeId(lk.getExtraChargeId())
                            .orderItemId(itemId)
                            .refundAmount(refundExtra)
                            .build();
                    orderRefundExtraChargesMapper.insert(rec);
                }

                itemExtraRefundSum.merge(itemId, refundExtra, BigDecimal::add);
            }
        }

        // 7) 订单级 extra：仅整单退款时退
        BigDecimal orderLevelExtraRefund = BigDecimal.ZERO;
        if (fullRefundAfterThis) {
            List<OrderExtraCharges> orderLevelCharges = orderExtraChargesMapper.selectList(
                    Wrappers.<OrderExtraCharges>lambdaQuery()
                            .eq(OrderExtraCharges::getOrderNo, orderNo)
                            .isNull(OrderExtraCharges::getOrderItemId));

            for (OrderExtraCharges ch : orderLevelCharges) {
                BigDecimal amt = ch.getChargeAmount() == null ? BigDecimal.ZERO : ch.getChargeAmount();
                orderLevelExtraRefund = orderLevelExtraRefund.add(amt);

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
                            .refundAmount(amt)
                            .build();
                    orderRefundExtraChargesMapper.insert(rec);
                }
            }
        }

        // 8) 生成退款事实（order_item_refunds）+ item 状态流转 + 日志，并汇总总退款金额
        int approvedCount = 0;
        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        for (OrderItems it : items) {
            Long itemId = it.getId();

            BigDecimal itemAmount = it.getUnitPrice();
            Assert.isNotEmpty(itemAmount, OrderCode.ORDER_ITEM_NOT_EXIST);

            BigDecimal percent = itemPercentMap.getOrDefault(itemId, BigDecimal.ZERO);

            // item 基础金额按比例退
            BigDecimal refundItemAmount = itemAmount.multiply(percent).divide(TOP, SCALE, RoundingMode.HALF_UP);

            // item 额外费用按比例退（来自上面的 itemExtraRefundSum）
            BigDecimal refundExtraAmount = itemExtraRefundSum.getOrDefault(itemId, BigDecimal.ZERO);

            BigDecimal refundAmount = refundItemAmount.add(refundExtraAmount);

            // “不可退部分/扣款”（你表字段叫 refund_fee）
            BigDecimal baseOrigin = itemAmount;
            if (links != null && !links.isEmpty()) {
                // origin extra 合计不再保存，这里用：refundFee = (itemAmount + originExtra) - refundAmount
                // 若你要精确记录 originExtra，可在上面额外维护 itemExtraOriginSum
                BigDecimal originExtra = BigDecimal.ZERO;
                for (OrderExtraChargeLinks lk : links) {
                    if (itemId.equals(lk.getOrderItemId())) {
                        originExtra = originExtra.add(lk.getAllocatedAmount() == null ? BigDecimal.ZERO : lk.getAllocatedAmount());
                    }
                }
                baseOrigin = baseOrigin.add(originExtra);
            }
            BigDecimal refundFee = baseOrigin.subtract(refundAmount);

            // 汇总总退款金额（按“实际退款金额”口径）
            totalRefundAmount = totalRefundAmount.add(refundAmount);

            // 幂等：同一个 apply + item 只插一次事实
            boolean factExists = orderItemRefundsMapper.exists(
                    Wrappers.<OrderItemRefunds>lambdaQuery()
                            .eq(OrderItemRefunds::getRefundApplyId, refundApplyId)
                            .eq(OrderItemRefunds::getOrderItemId, itemId));

            if (!factExists) {
                OrderItemRefunds fact = OrderItemRefunds.builder()
                        .orderNo(orderNo)
                        .orderItemId(itemId)
                        .refundApplyId(refundApplyId)
                        // 保留原始 itemAmount
                        .itemAmount(itemAmount)
                        // 本次实退 extra
                        .extraChargeAmount(refundExtraAmount)
                        .refundFee(refundFee)
                        .refundAmount(refundAmount)
                        .refundStatus(RefundStatusEnum.APPROVED)
                        .build();
                orderItemRefundsMapper.insert(fact);
            }

            // 仅对 WAIT_APPROVING 的做状态迁移
            if (it.getRefundStatus() == RefundStatusEnum.WAIT_APPROVING) {
                int ur = orderItemsMapper.update(
                        null,
                        Wrappers.<OrderItems>lambdaUpdate()
                                .eq(OrderItems::getId, itemId)
                                .eq(OrderItems::getOrderNo, orderNo)
                                .eq(OrderItems::getRefundStatus, RefundStatusEnum.WAIT_APPROVING)
                                .set(OrderItems::getRefundStatus, RefundStatusEnum.APPROVED));
                Assert.isTrue(ur == 1, OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);

                approvedCount++;

                OrderStatusLogs itemLog = OrderStatusLogs.builder()
                        .orderNo(orderNo)
                        .orderId(order.getId())
                        .orderItemId(itemId)
                        .action(OrderActionEnum.REFUND_APPROVE)
                        .oldOrderStatus(order.getOrderStatus())
                        .newOrderStatus(OrderStatusEnum.REFUNDING)
                        .oldItemRefundStatus(RefundStatusEnum.WAIT_APPROVING)
                        .newItemRefundStatus(RefundStatusEnum.APPROVED)
                        .refundApplyId(refundApplyId)
                        .operatorType(operatorType)
                        .operatorId(operatorId)
                        .operatorName(operatorType.getOperatorTypeName() + "_" + operatorId)
                        .remark(isAutoRefund ? "服务提供方自动审批退款" : "服务提供方同意退款")
                        .build();
                orderStatusLogsMapper.insert(itemLog);
            }
        }

        // 加上订单级 extra 退款（整单时）
        totalRefundAmount = totalRefundAmount.add(orderLevelExtraRefund);

        Assert.isTrue(totalRefundAmount.compareTo(BigDecimal.ZERO) >= 0, OrderCode.ORDER_REFUND_AMOUNT_INVALID);
        Assert.isTrue(totalRefundAmount.compareTo(order.getSubtotal()) <= 0, OrderCode.ORDER_REFUND_AMOUNT_INVALID);

        // 9) 更新退款申请状态：PENDING -> APPROVED（生成 outRequestNo）
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

        // 10) 订单状态：REFUNDING
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
                .operatorType(operatorType)
                .operatorId(operatorId)
                .operatorName(operatorType.getOperatorTypeName() + "_" + operatorId)
                .remark(isAutoRefund ? "服务提供方自动审批退款" : "服务提供方同意退款")
                .build();
        orderStatusLogsMapper.insert(orderLog);

        // 11) afterCommit：发起退款 + 解锁资源
        List<Long> recordIds = items.stream().map(OrderItems::getRecordId).toList();
        LocalDate bookingDate = items.get(0).getBookingDate();

        BigDecimal finalTotalRefundAmount = totalRefundAmount;

        boolean zeroPay = order.getPayAmount() != null
                && order.getPayAmount().compareTo(BigDecimal.ZERO) == 0;
        if (!zeroPay) {
            String remarkCodeDescription = apply.getReasonCode().getDescription();
            String refundReason = remarkCodeDescription + "-" + (operatorType.equals(OperatorTypeEnum.USER) ?   apply.getReasonDetail(): apply.getSellerRemark());
            // 直接发起退款 rpc
            UserRefundRequestDto requestDto = UserRefundRequestDto.builder()
                    .refundReason(refundReason)
                    .outTradeNo(order.getOutTradeNo())
                    .outRequestNo(outRequestNo)
                    .orderNo(order.getOrderNo())
                    .fullRefund(fullRefundAfterThis)
                    .refundAmount(finalTotalRefundAmount)
                    .build();
            RpcResult<Void> result = paymentForOrderDubboService.userRefund(requestDto);
            Assert.rpcResultOk(result);
        }

        // 退款状态修改（0 元订单直接走成功）
        thisService.refundSuccessAction(orderNo, outRequestNo);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {

                UnlockSlotMessage unlockMsg = UnlockSlotMessage.builder()
                        .orderNo(orderNo)
                        .userId(order.getBuyerId())
                        .operatorType(operatorType)
                        .recordIds(recordIds)
                        .isActivity(order.getActivity())
                        .bookingDate(bookingDate)
                        .build();

                if (SellerTypeEnum.VENUE.equals(sellerType)) {
                    mqService.send(
                            OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                            OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                            unlockMsg);
                } else if (SellerTypeEnum.COACH.equals(sellerType)) {
                    mqService.send(
                            OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_COACH_SLOT,
                            OrderMQConstants.ROUTING_ORDER_UNLOCK_COACH_SLOT,
                            unlockMsg);
                }
            }
        });

        return approvedCount;
    }


    @Transactional(rollbackFor = Exception.class)
    public void refundSuccessAction(Long orderNo, String outRequestNo) {
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


        // 10) 更新订单状态
        if (newOrderStatus != oldOrderStatus) {
            order.setOrderStatus(newOrderStatus);
            order.setRefundApplyId(refundApplyId);
            ordersMapper.updateById(order);

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

        // 13) 发送退款成功通知给用户
        if (SellerTypeEnum.VENUE.equals(order.getSellerType())) {
            venueNotificationHelper.sendVenueRefundSuccess(orderNo, order.getBuyerId(), apply.getRefundAmount());
        } else if (SellerTypeEnum.COACH.equals(order.getSellerType())) {
            coachNotificationHelper.sendCoachRefundSuccess(orderNo, order.getBuyerId(), apply.getRefundAmount());
        }
    }

    @Override
    public void refundSuccessMQHandler(PaymentRefundMessage message) {
        Assert.isNotEmpty(message, OrderCode.PARAM_ERROR);
        Long orderNo = message.getOrderNo();
        String outRequestNo = message.getOutRequestNo();

        Assert.isNotEmpty(orderNo, OrderCode.PARAM_ERROR);

        if (!message.isOrderCancelled()) {
            Assert.isTrue(outRequestNo != null && !outRequestNo.isBlank(), OrderCode.PARAM_ERROR);
        } else {
            // 订单已经到了被取消的状态，不需要其他操作
            return;
        }

        thisService.refundSuccessAction(orderNo, outRequestNo);
    }
}
