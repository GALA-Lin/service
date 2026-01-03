package com.unlimited.sports.globox.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.message.order.OrderNotifyMerchantConfirmMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.common.utils.AuthContextHolder;
import com.unlimited.sports.globox.model.order.dto.ApplyRefundRequestDto;
import com.unlimited.sports.globox.model.order.dto.CancelRefundApplyRequestDto;
import com.unlimited.sports.globox.model.order.dto.GetRefundProgressRequestDto;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.model.order.vo.*;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.lock.RedisLock;
import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.OrderRefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 提供订单退款相关的服务操作。
 */
@Slf4j
@Service
public class OrderRefundServiceImpl implements OrderRefundService {

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

    /**
     * 申请订单退款。
     * TODO ETA 2026/01/03 MQ 通知商家有新的退款申请
     *
     * @param dto 退款请求参数
     * @return 包含退款申请结果的统一响应对象
     */
    @Override
    @RedisLock(value = "#dto.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public ApplyRefundResultVo applyRefund(ApplyRefundRequestDto dto) {

        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        Long orderNo = dto.getOrderNo();
        LocalDateTime appliedAt = LocalDateTime.now();

        // 1. 查询订单
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getBuyerId, userId)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        // 2. 校验订单是否允许退款
        Assert.isTrue(order.getPaymentStatus() == OrdersPaymentStatusEnum.PAID,
                OrderCode.ORDER_NOT_ALLOW_REFUND);

        // 订单可申请退款条件
        Assert.isTrue(
                // 订单已支付
                order.getOrderStatus() == OrderStatusEnum.PAID
                        // 订单已确认
                        || order.getOrderStatus() == OrderStatusEnum.CONFIRMED
                        // 订单退款被拒绝
                        || order.getOrderStatus() == OrderStatusEnum.REFUND_REJECTED
                        // 部分退款申请完成
                        || order.getOrderStatus() == OrderStatusEnum.PARTIALLY_REFUNDED
                        // 退款已取消
                        || order.getOrderStatus() == OrderStatusEnum.REFUND_CANCELLED,
                OrderCode.ORDER_NOT_ALLOW_REFUND);

        // 3. 查询并校验申请的订单项
        List<Long> reqItemIds = dto.getOrderItemIds() == null
                ? List.of()
                : dto.getOrderItemIds().stream().filter(Objects::nonNull).distinct().toList();
        Assert.isTrue(!reqItemIds.isEmpty(), OrderCode.ORDER_ITEM_NOT_EXIST);

        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, reqItemIds));
        Assert.isTrue(items.size() == reqItemIds.size(), OrderCode.ORDER_ITEM_NOT_EXIST);

        // 教练订单不支持部分退款
        if (order.getSellerType().equals(SellerTypeEnum.COACH)) {
            Long allItemCount = orderItemsMapper.selectCount(
                    Wrappers.<OrderItems>lambdaQuery()
                            .eq(OrderItems::getOrderNo, orderNo));
            Assert.isTrue(reqItemIds.size() ==  allItemCount, OrderCode.COACH_ONLY_REFUND_ALL);
        }

        // 商家订单需要查询退款规则 TODO ETA 等一下林森
        if (order.getSellerType().equals(SellerTypeEnum.VENUE)) {

        }

        // 校验这些 item 当前是否可发起退款：必须是 NONE
        for (OrderItems item : items) {
            RefundStatusEnum st = item.getRefundStatus();
            assertItemRefundable(st);
        }

        // 4. 创建退款申请单
        OrderRefundApply refundApply = OrderRefundApply.builder()
                .orderNo(orderNo)
                .applyStatus(ApplyRefundStatusEnum.PENDING)
                .reasonCode(dto.getReasonCode())
                .reasonDetail(dto.getReasonDetail())
                .build();
        refundApply.setCreatedAt(appliedAt);

        orderRefundApplyMapper.insert(refundApply);
        Long refundApplyId = refundApply.getId();
        Assert.isNotEmpty(refundApplyId, OrderCode.ORDER_REFUND_APPLY_CREATE_FAILED);

        // 5. 写入本次申请包含哪些订单项（order_refund_apply_items）
        for (Long itemId : reqItemIds) {
            OrderRefundApplyItems applyItem = OrderRefundApplyItems.builder()
                    .refundApplyId(refundApplyId)
                    .orderNo(orderNo)
                    .orderItemId(itemId)
                    .build();
            orderRefundApplyItemsMapper.insert(applyItem);
        }

        // 6. 更新订单项退款状态：NONE -> WAIT_APPROVING（带条件更新，防并发）
        for (Long itemId : reqItemIds) {
            int ur = orderItemsMapper.update(
                    null,
                    Wrappers.<OrderItems>lambdaUpdate()
                            .eq(OrderItems::getId, itemId)
                            .eq(OrderItems::getOrderNo, orderNo)
                            .eq(OrderItems::getRefundStatus, RefundStatusEnum.NONE)
                            .set(OrderItems::getRefundStatus, RefundStatusEnum.WAIT_APPROVING));
            Assert.isTrue(ur == 1, OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
        }

        // 7. 更新订单状态：-> REFUND_APPLYING，并挂最新 refund_apply_id
        Orders updateOrder = new Orders();
        updateOrder.setId(order.getId());
        updateOrder.setOrderStatus(OrderStatusEnum.REFUND_APPLYING);
        updateOrder.setRefundApplyId(refundApplyId); // 注意：你现在 orders 字段名是 refund_apply_id
        int uo = ordersMapper.updateById(updateOrder);
        Assert.isTrue(uo == 1, OrderCode.ORDER_REFUND_APPLY_CREATE_FAILED);

        // 9. 写订单状态日志
        OrderStatusLogs logEntity = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.REFUND_APPLY)
                .oldOrderStatus(order.getOrderStatus())
                .newOrderStatus(OrderStatusEnum.REFUND_APPLYING)
                .refundApplyId(refundApplyId) // 你字段叫 refund_request_id；如果你想统一语义可后续改名
                .operatorType(OperatorTypeEnum.USER)
                .operatorId(userId)
                .operatorName("USER_" + userId)
                .remark("用户申请退款")
                .build();
        orderStatusLogsMapper.insert(logEntity);

        // 10. afterCommit MQ（可选）
        // TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        //     @Override
        //     public void afterCommit() {
        //         mqService.send(...通知商家...)
        //     }
        // });

        // 11. 返回结果
        return ApplyRefundResultVo.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.PENDING)
                .appliedAt(appliedAt)
                .build();
    }


    /**
     * 查询指定订单或退款申请的退款进度。
     *
     * @param dto 包含退款查询参数的对象，包括订单号和/或退款申请ID以及是否包含时间线
     * @return 包含退款进度详情的对象，包括订单状态、申请单状态、处理金额等信息
     */
    @Override
    @Transactional(readOnly = true)
    public GetRefundProgressVo getRefundProgress(GetRefundProgressRequestDto dto) {

        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        Long refundApplyId = dto.getRefundApplyId();
        Long orderNo = dto.getOrderNo();

        // 1) 定位订单 + 退款申请
        Orders order;
        OrderRefundApply apply;

        if (refundApplyId != null) {
            apply = orderRefundApplyMapper.selectById(refundApplyId);
            Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

            orderNo = apply.getOrderNo();
            Assert.isNotEmpty(orderNo, OrderCode.ORDER_NOT_EXIST);

            order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getOrderNo, orderNo)
                    .eq(Orders::getBuyerId, userId)
                    .last("LIMIT 1"));
            Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        } else {
            Assert.isNotEmpty(orderNo, OrderCode.ORDER_NOT_EXIST);

            order = ordersMapper.selectOne(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getOrderNo, orderNo)
                    .eq(Orders::getBuyerId, userId)
                    .last("LIMIT 1"));
            Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

            // 优先用 orders.refund_apply_id（用它代表“最新申请”）
            refundApplyId = order.getRefundApplyId();

            if (refundApplyId != null) {
                apply = orderRefundApplyMapper.selectById(refundApplyId);
            } else {
                // 兜底：按订单找最新一条申请
                apply = orderRefundApplyMapper.selectOne(Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .orderByDesc(OrderRefundApply::getId)
                        .last("LIMIT 1"));
                refundApplyId = (apply == null ? null : apply.getId());
            }

            Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
            Assert.isNotEmpty(refundApplyId, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }

        // 2) 取本次申请涉及的 itemId（apply_items）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));

        List<Long> itemIds = (applyItems == null || applyItems.isEmpty())
                ? List.of()
                : applyItems.stream().map(OrderRefundApplyItems::getOrderItemId).distinct().toList();

        // 3) 查询订单项 + 退款事实
        Map<Long, OrderItems> itemMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            List<OrderItems> items = orderItemsMapper.selectList(
                    Wrappers.<OrderItems>lambdaQuery()
                            .eq(OrderItems::getOrderNo, orderNo)
                            .in(OrderItems::getId, itemIds)
                            .orderByAsc(OrderItems::getId));
            for (OrderItems it : items) {
                itemMap.put(it.getId(), it);
            }
        }

        // 退款事实（可能为空：比如刚申请还没审批）
        Map<Long, OrderItemRefunds> refundFactMap = new HashMap<>();
        if (!itemIds.isEmpty()) {
            List<OrderItemRefunds> facts = orderItemRefundsMapper.selectList(
                    Wrappers.<OrderItemRefunds>lambdaQuery()
                            .eq(OrderItemRefunds::getOrderNo, orderNo)
                            .eq(OrderItemRefunds::getRefundApplyId, refundApplyId)
                            .in(OrderItemRefunds::getOrderItemId, itemIds));
            for (OrderItemRefunds f : facts) {
                refundFactMap.put(f.getOrderItemId(), f);
            }
        }

        // 4) extraCharges：本次申请记录的额外费用退款明细（order_refund_extra_charges）
        List<OrderRefundExtraCharges> refundExtras = orderRefundExtraChargesMapper.selectList(
                Wrappers.<OrderRefundExtraCharges>lambdaQuery()
                        .eq(OrderRefundExtraCharges::getOrderNo, orderNo)
                        .eq(OrderRefundExtraCharges::getRefundApplyId, refundApplyId)
                        .orderByAsc(OrderRefundExtraCharges::getId));

        Map<Long, OrderExtraCharges> extraChargeMap;
        if (refundExtras != null && !refundExtras.isEmpty()) {
            List<Long> extraIds = refundExtras.stream()
                    .map(OrderRefundExtraCharges::getExtraChargeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (!extraIds.isEmpty()) {
                extraChargeMap = orderExtraChargesMapper.selectBatchIds(extraIds).stream()
                        .collect(Collectors.toMap(OrderExtraCharges::getId, c -> c));
            } else {
                extraChargeMap = Map.of();
            }
        } else {
            extraChargeMap = Map.of();
        }

        // 由于 order_refund_extra_charges 没有 refund_status，这里用 apply 状态推导
        RefundStatusEnum extraRefundStatus;
        if (apply.getRefundCompletedAt() != null) {
            extraRefundStatus = RefundStatusEnum.COMPLETED;
        } else if (apply.getApplyStatus() == ApplyRefundStatusEnum.PENDING) {
            extraRefundStatus = RefundStatusEnum.WAIT_APPROVING;
        } else if (apply.getApplyStatus() == ApplyRefundStatusEnum.APPROVED) {
            extraRefundStatus = RefundStatusEnum.APPROVED;
        } else {
            extraRefundStatus = RefundStatusEnum.NONE;
        }

        List<RefundExtraChargeProgressVo> extraVos = (refundExtras == null ? List.of()
                : refundExtras.stream().map(re -> {
            OrderExtraCharges ch = extraChargeMap.get(re.getExtraChargeId());
            return RefundExtraChargeProgressVo.builder()
                    .extraChargeId(re.getExtraChargeId())
                    .orderItemId(re.getOrderItemId())
                    .refundAmount(re.getRefundAmount())
                    .chargeName(ch == null ? null : ch.getChargeName())
                    .refundStatus(extraRefundStatus)
                    .refundStatusName(extraRefundStatus.getDescription())
                    .build();
        }).toList());

        // 5) items 进度 VO
        List<RefundItemProgressVo> itemVos = new ArrayList<>();
        // 不要依赖 subtotal
        BigDecimal itemBaseSum = BigDecimal.ZERO;
        BigDecimal feeSum = BigDecimal.ZERO;
        BigDecimal completedItemRefundSum = BigDecimal.ZERO;

        for (Long itemId : itemIds) {
            OrderItems it = itemMap.get(itemId);
            if (it == null) continue;

            OrderItemRefunds fact = refundFactMap.get(itemId);

            RefundStatusEnum itemRefundStatus = it.getRefundStatus();
            String itemRefundStatusName = itemRefundStatus == null ? null : itemRefundStatus.getDescription();

            BigDecimal itemAmount = (fact != null ? fact.getItemAmount() : it.getUnitPrice());
            BigDecimal extraAmount = (fact != null ? fact.getExtraChargeAmount() : it.getExtraAmount());
            BigDecimal refundFee = (fact != null ? fact.getRefundFee() : null);
            BigDecimal refundAmount = (fact != null ? fact.getRefundAmount() : null);

            if (itemAmount != null) itemBaseSum = itemBaseSum.add(itemAmount);
            if (refundFee != null) feeSum = feeSum.add(refundFee);

            if (fact != null && fact.getRefundStatus() == RefundStatusEnum.COMPLETED && fact.getRefundAmount() != null) {
                completedItemRefundSum = completedItemRefundSum.add(fact.getRefundAmount());
            }

            itemVos.add(RefundItemProgressVo.builder()
                    .orderItemId(itemId)
                    .resourceName(it.getResourceName())
                    .refundStatus(itemRefundStatus)
                    .refundStatusName(itemRefundStatusName)
                    .itemAmount(itemAmount)
                    .extraChargeAmount(extraAmount)
                    .refundFee(refundFee)
                    .refundAmount(refundAmount)
                    .build());
        }

        // 6) 汇总金额：totalRefundAmount / refundedAmount / refundingAmount
        // extraSum：优先用本次申请写入的 order_refund_extra_charges；如果没有（如还没审批），按规则预估
        BigDecimal extraSum = extraVos.stream()
                .map(RefundExtraChargeProgressVo::getRefundAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 如果还没审批，extraVos 为空，也需要展示“预估额外费用”
        if ((refundExtras == null || refundExtras.isEmpty()) && !itemIds.isEmpty()) {
            // item 级额外费用：order_extra_charge_links.allocated_amount（只统计本次 itemIds 绑定的）
            List<OrderExtraChargeLinks> links = orderExtraChargeLinksMapper.selectList(
                    Wrappers.<OrderExtraChargeLinks>lambdaQuery()
                            .eq(OrderExtraChargeLinks::getOrderNo, orderNo)
                            .in(OrderExtraChargeLinks::getOrderItemId, itemIds));
            BigDecimal itemExtraEstimated = (links == null ? BigDecimal.ZERO
                    : links.stream()
                    .map(OrderExtraChargeLinks::getAllocatedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            // 若本次申请覆盖整单，则按规则把订单级 extra 也算进“应退预估”
            Long totalItemCnt = orderItemsMapper.selectCount(
                    Wrappers.<OrderItems>lambdaQuery().eq(OrderItems::getOrderNo, orderNo));
            boolean fullApply = (totalItemCnt != null && totalItemCnt > 0 && itemIds.size() == totalItemCnt.intValue());

            BigDecimal orderLevelExtraEstimated = BigDecimal.ZERO;
            if (fullApply) {
                List<OrderExtraCharges> orderLevelCharges = orderExtraChargesMapper.selectList(
                        Wrappers.<OrderExtraCharges>lambdaQuery()
                                .eq(OrderExtraCharges::getOrderNo, orderNo)
                                .isNull(OrderExtraCharges::getOrderItemId));
                orderLevelExtraEstimated = (orderLevelCharges == null ? BigDecimal.ZERO
                        : orderLevelCharges.stream()
                        .map(OrderExtraCharges::getChargeAmount)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }

            extraSum = itemExtraEstimated.add(orderLevelExtraEstimated);
        }

        BigDecimal totalRefundAmount = itemBaseSum.add(extraSum).subtract(feeSum);
        if (totalRefundAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalRefundAmount = BigDecimal.ZERO;
        }

        // refundedAmount：若整单申请已到账，直接等于 totalRefundAmount；否则用已完成的 item 退款累计
        BigDecimal refundedAmount = (apply.getRefundCompletedAt() != null)
                ? totalRefundAmount
                : completedItemRefundSum;

        BigDecimal refundingAmount = totalRefundAmount.subtract(refundedAmount);
        if (refundingAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundingAmount = BigDecimal.ZERO;
        }

        // 7) timeline（可选）
        List<RefundTimelineVo> timeline = List.of();
        if (Boolean.TRUE.equals(dto.getIncludeTimeline())) {
            List<OrderStatusLogs> logs = orderStatusLogsMapper.selectList(
                    Wrappers.<OrderStatusLogs>lambdaQuery()
                            .eq(OrderStatusLogs::getOrderNo, orderNo)
                            .eq(OrderStatusLogs::getRefundApplyId, refundApplyId)
                            .in(OrderStatusLogs::getAction,
                                    OrderActionEnum.REFUND_APPLY,
                                    OrderActionEnum.REFUND_APPROVE,
                                    OrderActionEnum.REFUND_REJECT,
                                    OrderActionEnum.REFUND_COMPLETE)
                            .orderByAsc(OrderStatusLogs::getCreatedAt));

            timeline = (logs == null ? List.<RefundTimelineVo>of() : logs.stream().map(statusLogs -> RefundTimelineVo.builder()
                    .action(statusLogs.getAction())
                    .actionName(statusLogs.getAction().getDescription())
                    .at(statusLogs.getCreatedAt())
                    .remark(statusLogs.getRemark())
                    .operatorType(statusLogs.getOperatorType())
                    .operatorId(statusLogs.getOperatorId())
                    .operatorName(statusLogs.getOperatorName())
                    .build()).toList());
        }

        // 8) 返回
        return GetRefundProgressVo.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .orderStatus(order.getOrderStatus())
                .orderStatusName(order.getOrderStatus().getDescription())
                .applyStatus(apply.getApplyStatus())
                .applyStatusName(apply.getApplyStatus().getDescription())
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
                .items(itemVos)
                .extraCharges(extraVos)
                .timeline(timeline)
                .build();
    }


    /**
     * 取消用户的退款申请。
     *
     * @param dto 包含取消退款请求参数的对象，包括订单号、退款申请ID以及可选的取消原因
     * @return 包含取消退款结果的信息对象，包括订单号、退款申请ID、申请单状态、订单状态、取消时间等信息
     */
    @Override
    @RedisLock(value = "#dto.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public CancelRefundApplyResultVo cancelRefundApply(CancelRefundApplyRequestDto dto) {

        Long userId = AuthContextHolder.getLongHeader(RequestHeaderConstants.HEADER_USER_ID);
        Assert.isNotEmpty(userId, UserAuthCode.TOKEN_EXPIRED);

        Long orderNo = dto.getOrderNo();
        Long refundApplyId = dto.getRefundApplyId();
        LocalDateTime now = LocalDateTime.now();

        // 1) 锁订单（必须是本人订单）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getBuyerId, userId)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(order, OrderCode.ORDER_NOT_EXIST);

        // 2) 锁退款申请（必须属于该订单）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        Assert.isNotEmpty(apply, OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);

        // 3) 幂等：已取消直接返回
        if (apply.getApplyStatus() == ApplyRefundStatusEnum.CANCELLED) {
            OrderStatusEnum st = order.getOrderStatus();
            return CancelRefundApplyResultVo.builder()
                    .orderNo(orderNo)
                    .refundApplyId(refundApplyId)
                    .applyStatus(ApplyRefundStatusEnum.CANCELLED)
                    .applyStatusName(ApplyRefundStatusEnum.CANCELLED.getDescription())
                    .orderStatus(st)
                    .orderStatusName(st.getDescription())
                    .cancelledAt(now) // 没有 cancelled_at 字段时，这里只能返回当前；建议以后按日志时间展示
                    .build();
        }

        // 4) 只能取消 PENDING 的申请（避免与商家审批并发）
        Assert.isTrue(apply.getApplyStatus() == ApplyRefundStatusEnum.PENDING,
                OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);

        // 5) 取出本次申请包含的 itemId（精确绑定）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo));
        Assert.isNotEmpty(applyItems, OrderCode.ORDER_ITEM_NOT_EXIST);

        List<Long> applyItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Assert.isTrue(!applyItemIds.isEmpty(), OrderCode.ORDER_ITEM_NOT_EXIST);

        // 6) 恢复订单项退款状态：WAIT_APPROVING -> NONE（只允许恢复本次申请的 items）
        //    若其中有任何一个不是 WAIT_APPROVING，说明出现并发审批/状态被推进，直接拒绝取消
        int restored = orderItemsMapper.update(
                null,
                Wrappers.<OrderItems>lambdaUpdate()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .eq(OrderItems::getRefundStatus, RefundStatusEnum.WAIT_APPROVING)
                        .set(OrderItems::getRefundStatus, RefundStatusEnum.NONE));
        Assert.isTrue(restored == applyItemIds.size(), OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);

        // 7) 更新退款申请状态：PENDING -> CANCELLED（条件更新确保并发安全）
        int ua = orderRefundApplyMapper.update(
                null,
                Wrappers.<OrderRefundApply>lambdaUpdate()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .eq(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.PENDING)
                        .set(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.CANCELLED)
        );
        Assert.isTrue(ua == 1, OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);

        // 8) 订单状态更改
        Orders updOrder = new Orders();
        updOrder.setId(order.getId());
        updOrder.setOrderStatus(OrderStatusEnum.REFUND_CANCELLED);
        ordersMapper.updateById(updOrder);

        // 9) 写订单级日志
        OrderStatusLogs cancelLog = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.REFUND_CANCEL)
                .oldOrderStatus(order.getOrderStatus())
                .newOrderStatus(OrderStatusEnum.REFUND_CANCELLED)
                .refundApplyId(refundApplyId)
                .operatorType(OperatorTypeEnum.USER)
                .operatorId(userId)
                .operatorName("USER_" + userId)
                .remark("用户取消退款申请")
                .build();
        orderStatusLogsMapper.insert(cancelLog);

        // 10) 如果订单还没有确认，那么发送通知商家确认消息
        if (!order.isConfirmed()) {
            OrderNotifyMerchantConfirmMessage confirmMessage = OrderNotifyMerchantConfirmMessage.builder()
                    .orderNo(orderNo)
                    .venueId(order.getSellerId())
                    .currentOrderStatus(OrderStatusEnum.REFUND_CANCELLED)
                    .build();
            mqService.send(
                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_CONFIRM_NOTIFY_MERCHANT,
                    OrderMQConstants.ROUTING_ORDER_CONFIRM_NOTIFY_MERCHANT,
                    confirmMessage);
        }

        return CancelRefundApplyResultVo.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.CANCELLED)
                .applyStatusName(ApplyRefundStatusEnum.CANCELLED.getDescription())
                .orderStatus(OrderStatusEnum.REFUND_CANCELLED)
                .orderStatusName(OrderStatusEnum.REFUND_CANCELLED.getDescription())
                .cancelledAt(now)
                .build();
    }

    private void assertItemRefundable(RefundStatusEnum st) {
        Assert.isTrue(
                st == RefundStatusEnum.NONE, () -> {
                    return switch (st) {
                        case WAIT_APPROVING -> OrderCode.ORDER_REFUND_ALREADY_PENDING;
                        case APPROVED -> OrderCode.ORDER_REFUND_ALREADY_APPROVED;
                        case COMPLETED -> OrderCode.ORDER_REFUND_ALREADY_COMPLETED;
                        default -> OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID;
                    };
                });
    }
}
