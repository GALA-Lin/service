package com.unlimited.sports.globox.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.order.dto.*;
import com.unlimited.sports.globox.model.order.entity.*;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.mapper.*;
import com.unlimited.sports.globox.order.service.OrderDubboService;
import com.unlimited.sports.globox.order.service.OrderRefundActionService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * 订单模块远程服务通用功能
 */
@Slf4j
@Service
public class OrderDubboServiceImpl implements OrderDubboService {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private OrderRefundApplyMapper orderRefundApplyMapper;

    @Autowired
    private OrderRefundApplyItemsMapper orderRefundApplyItemsMapper;

    @Autowired
    private OrderRefundActionService orderRefundActionService;

    @Autowired
    private MQService mqService;

    @Autowired
    private ExecutorService businessExecutorService;

    /**
     * 服务提供方取消未支付订单
     *
     * @param orderNo    订单号
     * @param sellerId   服务方 id
     * @param sellerType 服务方类型
     * @return 结果
     */
    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<SellerCancelOrderResultDto> sellerCancelUnpaidOrder(Long orderNo, Long sellerId, Long operatorId, SellerTypeEnum sellerType) {
        // 1) 查询订单（行锁，防并发支付/取消）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, sellerId)
                        .last("FOR UPDATE"));

        // 订单必须存在
        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        // 2) 幂等：如果已取消，直接返回成功
        if (order.getOrderStatus() == OrderStatusEnum.CANCELLED) {
            SellerCancelOrderResultDto resultDto = SellerCancelOrderResultDto.builder()
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
                .operatorId(sellerId)
                .operatorName(sellerType.getDescription() + "_" + sellerId)
                .remark("服务提供方取消未支付订单")
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
                if (SellerTypeEnum.VENUE.equals(sellerType)) {
                    UnlockSlotMessage unlockMsg = UnlockSlotMessage.builder()
                            .orderNo(orderNo)
                            .userId(order.getBuyerId())
                            .operatorType(OperatorTypeEnum.MERCHANT)
                            .recordIds(recordIds)
                            .isActivity(order.getActivity())
                            .bookingDate(bookingDate)
                            .build();

                    mqService.send(
                            OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                            OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                            unlockMsg);
                } else if (SellerTypeEnum.COACH.equals(sellerType)) {
                    // TODO 教练取消

                }
            }
        });

        // 7) 返回
        SellerCancelOrderResultDto resultDto = SellerCancelOrderResultDto.builder()
                .orderNo(orderNo)
                .success(true)
                .orderStatus(OrderStatusEnum.CANCELLED)
                .orderStatusName(OrderStatusEnum.CANCELLED.getDescription())
                .cancelledAt(now)
                .build();

        return RpcResult.ok(resultDto);
    }


    /**
     * 服务提供方确认订单的方法。
     *
     * @param orderNo     订单号
     * @param autoConfirm 是否自动确认
     * @param operatorId  操作员ID
     * @param sellerType  服务提供方类型
     * @return 返回商家确认订单的结果，包括订单号、是否确认成功、当前订单状态、状态描述以及确认时间
     */
    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<SellerConfirmResultDto> sellerConfirm(Long orderNo, boolean autoConfirm, Long operatorId, SellerTypeEnum sellerType) {
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
        // 2) 只有 已支付才需要确认订单
        if (currentOrderStatus == OrderStatusEnum.PAID) {
            int updated = ordersMapper.update(
                    null,
                    Wrappers.<Orders>lambdaUpdate()
                            .eq(Orders::getOrderNo, orderNo)
                            .eq(Orders::getOrderStatus, OrderStatusEnum.PAID)
                            .set(Orders::getOrderStatus, OrderStatusEnum.CONFIRMED));
            if (updated > 0) {
                // 3) 记录订单日志表
                OrderStatusLogs statusLogs = OrderStatusLogs.builder()
                        .orderId(orders.getId())
                        .orderNo(orders.getOrderNo())
                        .action(OrderActionEnum.CONFIRM)
                        .oldOrderStatus(currentOrderStatus)
                        .newOrderStatus(newOrderStatus)
                        .operatorType(autoConfirm ? OperatorTypeEnum.SYSTEM : OperatorTypeEnum.MERCHANT)
                        .operatorName(autoConfirm ?
                                OperatorTypeEnum.SYSTEM.getOperatorTypeName() : sellerType.getDescription() + "_" + operatorId)
                        .operatorId(autoConfirm ? null : operatorId)
                        .remark("订单已被服务提供方确认")
                        .build();

                orderStatusLogsMapper.insert(statusLogs);
            }
        }

        SellerConfirmResultDto resultDto = SellerConfirmResultDto.builder()
                .orderNo(orderNo)
                .orderStatus(newOrderStatus)
                .confirmAt(now)
                .success(true)
                .orderStatusName(newOrderStatus.getDescription())
                .build();
        return RpcResult.ok(resultDto);
    }


    /**
     * 商家批准退款申请。
     *
     * @param orderNo          订单号
     * @param sellerId         商家ID
     * @param refundApplyId    退款申请ID
     * @param sellerType       商家类型
     * @param refundPercentage 退款百分比
     * @return 返回一个RpcResult对象，包含商家批准退款的结果信息，包括订单状态、退款申请状态等
     */
    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<SellerApproveRefundResultDto> sellerApproveRefund(Long orderNo, Long sellerId, Long refundApplyId, SellerTypeEnum sellerType, BigDecimal refundPercentage) {
        LocalDateTime now = LocalDateTime.now();
        // 1) 锁订单（防并发取消/支付/重复审批）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, sellerId)
                        .last("FOR UPDATE"));
        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }
        if (!order.getOrderStatus().equals(OrderStatusEnum.REFUND_APPLYING)) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }

        // 2) 锁退款申请（必须属于该订单）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        if (ObjectUtils.isEmpty(apply)) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }

        // 3) 幂等：已同意直接返回
        if (apply.getApplyStatus() == ApplyRefundStatusEnum.APPROVED) {
            SellerApproveRefundResultDto resultDto = SellerApproveRefundResultDto.builder()
                    .orderNo(orderNo)
                    .refundApplyId(refundApplyId)
                    .applyStatus(apply.getApplyStatus())
                    .reviewedAt(apply.getReviewedAt())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getDescription())
                    .approvedItemCount(0)
                    .build();
            return RpcResult.ok(resultDto);
        }

        // 只允许 PENDING -> APPROVED
        if (apply.getApplyStatus() != ApplyRefundStatusEnum.PENDING) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);
        }

        // 4) 取出本次申请的 itemId（用 apply_items 精确绑定）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));
        if (ObjectUtils.isEmpty(applyItems)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        List<Long> applyItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ObjectUtils.isEmpty(applyItemIds)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        // 5) 查订单项并校验：必须属于该订单，且状态只能是 WAIT_APPROVING 或 APPROVED（幂等/重试安全）
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .orderByAsc(OrderItems::getId));
        if (items.size() != applyItemIds.size()) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        for (OrderItems it : items) {
            RefundStatusEnum st = it.getRefundStatus();
            if (ObjectUtils.isEmpty(st)) {
                return RpcResult.error(OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
            }
            if (st != RefundStatusEnum.WAIT_APPROVING && st != RefundStatusEnum.APPROVED) {
                return RpcResult.error(OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
            }
        }

        int itemCount = orderRefundActionService.refundAction(orderNo,
                refundApplyId,
                false,
                sellerId,
                OperatorTypeEnum.USER,
                sellerType);

        SellerApproveRefundResultDto resultDto = SellerApproveRefundResultDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.APPROVED)
                .reviewedAt(now)
                .orderStatus(OrderStatusEnum.REFUNDING)
                .orderStatusName(OrderStatusEnum.REFUNDING.getDescription())
                .approvedItemCount(itemCount)
                .build();
        return RpcResult.ok(resultDto);
    }


    /**
     * 服务提供方拒绝退款申请的方法。
     *
     * @param orderNo       订单号
     * @param refundApplyId 退款申请ID
     * @param sellerId      服务提供方ID
     * @param operatorId    操作员ID
     * @param sellerType    服务提供方类型
     * @param remark        备注信息，说明拒绝原因
     * @return 返回一个RpcResult对象，包含服务提供方拒绝退款的结果信息，包括订单状态、退款申请状态等
     */
    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<SellerRejectRefundResultDto> rejectRefund(Long orderNo, Long refundApplyId, Long sellerId, Long operatorId, SellerTypeEnum sellerType, String remark) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 锁订单（防并发取消/支付/重复审批）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, sellerId)
                        .last("FOR UPDATE"));
        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }
        if (!order.getOrderStatus().equals(OrderStatusEnum.REFUND_APPLYING)) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }

        // 2) 锁退款申请（必须属于该订单）
        OrderRefundApply apply = orderRefundApplyMapper.selectOne(
                Wrappers.<OrderRefundApply>lambdaQuery()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getOrderNo, orderNo)
                        .last("FOR UPDATE"));
        if (ObjectUtils.isEmpty(apply)) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_NOT_EXIST);
        }

        // 3) 幂等：已拒绝直接返回
        if (apply.getApplyStatus() == ApplyRefundStatusEnum.REJECTED) {
            SellerRejectRefundResultDto resultDto = SellerRejectRefundResultDto.builder()
                    .orderNo(orderNo)
                    .refundApplyId(refundApplyId)
                    .applyStatus(ApplyRefundStatusEnum.REJECTED)
                    .reviewedAt(apply.getReviewedAt())
                    .orderStatus(order.getOrderStatus())
                    .orderStatusName(order.getOrderStatus().getDescription())
                    .rejectedItemCount(0)
                    .build();
            return RpcResult.ok(resultDto);
        }

        // 只允许从 PENDING -> REJECTED（已 APPROVED 就不允许拒绝了）
        if (apply.getApplyStatus() != ApplyRefundStatusEnum.PENDING) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);
        }

        // 4) 取出本次申请涉及的 itemId（精准绑定）
        List<OrderRefundApplyItems> applyItems = orderRefundApplyItemsMapper.selectList(
                Wrappers.<OrderRefundApplyItems>lambdaQuery()
                        .eq(OrderRefundApplyItems::getRefundApplyId, refundApplyId)
                        .eq(OrderRefundApplyItems::getOrderNo, orderNo)
                        .orderByAsc(OrderRefundApplyItems::getId));
        if (ObjectUtils.isEmpty(applyItems)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        List<Long> applyItemIds = applyItems.stream()
                .map(OrderRefundApplyItems::getOrderItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ObjectUtils.isEmpty(applyItemIds)) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        // 5) 查询订单项并校验必须处于 WAIT_APPROVING（否则就是串单/并发/重复处理）
        List<OrderItems> items = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .orderByAsc(OrderItems::getId));
        if (items.size() != applyItemIds.size()) {
            return RpcResult.error(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        for (OrderItems it : items) {
            if (it.getRefundStatus() != RefundStatusEnum.WAIT_APPROVING) {
                return RpcResult.error(OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
            }
        }

        // 6) 更新申请状态：PENDING -> REJECTED
        int ua = orderRefundApplyMapper.update(
                null,
                Wrappers.<OrderRefundApply>lambdaUpdate()
                        .eq(OrderRefundApply::getId, refundApplyId)
                        .eq(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.PENDING)
                        .set(OrderRefundApply::getApplyStatus, ApplyRefundStatusEnum.REJECTED)
                        .set(OrderRefundApply::getReviewedAt, now)
                        .set(OrderRefundApply::getSellerRemark, remark));
        if (ua != 1) {
            return RpcResult.error(OrderCode.ORDER_REFUND_APPLY_STATUS_NOT_ALLOW);
        }

        // 7) 更新订单项：WAIT_APPROVING -> NONE（只更新本次申请的 itemId 集合）
        //    你说“只能全部拒绝”，所以这里必须全部更新成功
        int updatedCnt = orderItemsMapper.update(
                null,
                Wrappers.<OrderItems>lambdaUpdate()
                        .eq(OrderItems::getOrderNo, orderNo)
                        .in(OrderItems::getId, applyItemIds)
                        .eq(OrderItems::getRefundStatus, RefundStatusEnum.WAIT_APPROVING)
                        .set(OrderItems::getRefundStatus, RefundStatusEnum.NONE));
        if (updatedCnt != applyItemIds.size()) {
            return RpcResult.error(OrderCode.ORDER_ITEM_REFUND_STATUS_INVALID);
        }

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
                    .operatorId(operatorId)
                    .operatorName(sellerType.getDescription() + "_" + operatorId)
                    .remark(ObjectUtils.isEmpty(remark) ? "商家拒绝退款申请" : remark)
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
                .operatorId(operatorId)
                .operatorName(sellerType.getDescription() + "_" + operatorId)
                .remark(ObjectUtils.isEmpty(remark) ? "商家拒绝退款申请" : remark)
                .build();
        orderStatusLogsMapper.insert(orderLog);

        SellerRejectRefundResultDto resultDto = SellerRejectRefundResultDto.builder()
                .orderNo(orderNo)
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.REJECTED)
                .reviewedAt(now)
                .orderStatus(OrderStatusEnum.REFUND_REJECTED)
                .orderStatusName(OrderStatusEnum.REFUND_REJECTED.getDescription())
                .rejectedItemCount(applyItemIds.size())
                .build();

        return RpcResult.ok(resultDto);
    }


    /**
     * 服务提供方发起退款的方法。
     *
     * @param orderNo    订单号
     * @param SellerId   服务提供方ID
     * @param operatorId 操作员ID
     * @param reqItemIds 请求的订单项ID列表
     * @param sellerType 服务提供方类型
     * @param remark     备注信息，说明退款原因
     * @return 返回一个RpcResult对象，包含服务提供方发起退款的结果信息，包括订单状态、退款申请状态等
     */
    @Override
    @RedisLock(value = "#orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    public RpcResult<SellerRefundResultDto> refund(Long orderNo, Long SellerId, Long operatorId, List<Long> reqItemIds, SellerTypeEnum sellerType, String remark) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 锁订单（防并发取消/支付/重复审批）
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .eq(Orders::getSellerId, SellerId)
                        .last("FOR UPDATE"));
        if (ObjectUtils.isEmpty(order)) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        if (order.getSellerType() != sellerType) {
            return RpcResult.error(OrderCode.ORDER_NOT_EXIST);
        }

        OrderStatusEnum currentOrderStatus = order.getOrderStatus();
        if (currentOrderStatus.equals(OrderStatusEnum.PENDING)
                || currentOrderStatus.equals(OrderStatusEnum.REFUNDING)
                || currentOrderStatus.equals(OrderStatusEnum.REFUND_APPLYING)
                || currentOrderStatus.equals(OrderStatusEnum.CANCELLED)
                || currentOrderStatus.equals(OrderStatusEnum.REFUNDED)
        ) {
            return RpcResult.error(OrderCode.ORDER_NOT_ALLOW_REFUND);
        }

        // 2) 构造申请退款记录
        OrderRefundApply refundApply = OrderRefundApply.builder()
                .orderNo(orderNo)
                .applyStatus(ApplyRefundStatusEnum.PENDING)
                .reasonCode(UserRefundReasonEnum.NONE)
                .reviewedAt(now)
                .sellerRemark(remark)
                .creator(ApplyRefundCreatorEnum.MERCHANT)
                .build();

        // 插入订单申请项
        orderRefundApplyMapper.insert(refundApply);
        Long refundApplyId = refundApply.getId();
        Assert.isNotEmpty(refundApplyId, OrderCode.ORDER_REFUND_APPLY_CREATE_FAILED);


        // 3)  写入本次申请包含哪些订单项（order_refund_apply_items）
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

        // 8. 写订单状态日志
        OrderStatusLogs logEntity = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.REFUND_APPLY)
                .oldOrderStatus(order.getOrderStatus())
                .newOrderStatus(OrderStatusEnum.REFUND_APPLYING)
                .refundApplyId(refundApplyId)
                .operatorType(OperatorTypeEnum.MERCHANT)
                .operatorId(operatorId)
                .operatorName(sellerType.getDescription() + "_" + operatorId)
                .remark("服务提供方发起退款")
                .build();
        orderStatusLogsMapper.insert(logEntity);

        orderRefundActionService.refundAction(orderNo,
                refundApplyId,
                true,
                operatorId,
                SellerTypeEnum.COACH.equals(order.getSellerType()) ? OperatorTypeEnum.COACH : OperatorTypeEnum.MERCHANT,
                order.getSellerType());

        SellerRefundResultDto resultDto = SellerRefundResultDto.builder()
                .orderNo(orderNo)
                .orderStatus(OrderStatusEnum.REFUNDING)
                .orderStatusName(OrderStatusEnum.REFUNDING.getDescription())
                .refundApplyId(refundApplyId)
                .applyStatus(ApplyRefundStatusEnum.APPROVED)
                .build();

        return RpcResult.ok(resultDto);
    }
}
