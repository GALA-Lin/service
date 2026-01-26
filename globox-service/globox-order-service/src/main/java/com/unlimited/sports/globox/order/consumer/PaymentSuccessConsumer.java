package com.unlimited.sports.globox.order.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.order.OrderAutoCompleteMessage;
import com.unlimited.sports.globox.common.message.order.OrderPaidMessage;
import com.unlimited.sports.globox.common.message.order.UserRefundMessage;
import com.unlimited.sports.globox.common.message.payment.PaymentSuccessMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.LocalDateUtils;
import com.unlimited.sports.globox.model.order.entity.OrderActivities;
import com.unlimited.sports.globox.model.order.entity.OrderItems;
import com.unlimited.sports.globox.model.order.entity.OrderStatusLogs;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.mapper.OrderActivitiesMapper;
import com.unlimited.sports.globox.order.mapper.OrderItemsMapper;
import com.unlimited.sports.globox.order.mapper.OrderStatusLogsMapper;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import com.unlimited.sports.globox.order.util.CoachNotificationHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 支付成功回调
 */
@Slf4j
@Component
public class PaymentSuccessConsumer {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private MQService mqService;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderActivitiesMapper orderActivitiesMapper;

    @Autowired
    private CoachNotificationHelper coachNotificationHelper;

    /**
     * 支付成功回调 消费者
     */
    @RabbitListener(queues = PaymentMQConstants.QUEUE_PAYMENT_SUCCESS_ORDER)
    @Transactional(rollbackFor = Exception.class)
    @RedisLock(value = "#message.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @RabbitRetryable(
            finalExchange = PaymentMQConstants.EXCHANGE_PAYMENT_SUCCESS_FINAL_DLX,
            finalRoutingKey = PaymentMQConstants.ROUTING_PAYMENT_SUCCESS_FINAL,
            bizKey = "#message.orderNo",
            bizType = MQBizTypeEnum.PAYMENT_SUCCESS
    )
    public void onMessage(
            PaymentSuccessMessage message,
            Channel channel,
            Message amqpMessage) {
        Long orderNo = message.getOrderNo();
        log.info("[支付成功回调] 接收到消息 orderNo={}, tradeNo={}, payType={}, amount={}, paidAt={}",
                orderNo,
                message.getTradeNo(),
                message.getPaymentType(),
                message.getTotalAmount(),
                message.getPaymentAt());

        // 1) 基础校验（必要字段）
        if (orderNo == null) {
            throw new IllegalArgumentException("[支付成功回调] orderNo 为空");
        }
        if (message.getTradeNo() == null || message.getTradeNo().isBlank()) {
            throw new IllegalArgumentException("[支付成功回调] tradeNo 为空");
        }
        if (message.getPaymentType() == null) {
            throw new IllegalArgumentException("[支付成功回调] paymentType 为空");
        }
        if (message.getTotalAmount() == null) {
            throw new IllegalArgumentException("[支付成功回调] totalAmount 为空");
        }
        if (message.getPaymentAt() == null) {
            // 没带就用当前时间也行；但你要严格就直接抛错走重试
            message.setPaymentAt(LocalDateTime.now());
        }

        // 2) 查订单并加行锁，防并发重复消费
        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .last("FOR UPDATE"));

        if (order == null) {
            // 订单不存在：通常属于数据不一致/乱序消息（你也可以选择直接 return 当成消费成功）
            throw new IllegalStateException("[支付成功回调] 订单不存在 orderNo=" + orderNo);
        }

        // 3) 幂等：已经支付成功就直接忽略（ACK 由 AOP 做）
        //    这里假设你有：OrdersPaymentStatusEnum.PAID / UNPAID 等
        if (order.getPaymentStatus() == OrdersPaymentStatusEnum.PAID) {
            // 如果你存了 tradeNo，可在这里做一致性校验（不同 tradeNo 可能是异常）
            log.info("[支付成功回调] 订单已是已支付状态，幂等忽略 orderNo={}, existTradeNo={}",
                    orderNo, order.getTradeNo());
            return;
        }

        // 4) 状态前置校验，必须 pending
        if (order.getOrderStatus() != OrderStatusEnum.PENDING) {
            if (order.getOutTradeNo() != null && message.getOutTradeNo().equals(order.getOutTradeNo())) {
                // 同一个支付信息，已处理直接返回
                return;
            }

            String outRequestNo = UUID.randomUUID().toString().replace("-", "");
            if (order.getOrderStatus().equals(OrderStatusEnum.CANCELLED)) {
                // 需要触发退款
                UserRefundMessage refundMessage = UserRefundMessage.builder()
                        .orderNo(orderNo)
                        .outTradeNo(message.getOutTradeNo())
                        .outRequestNo(outRequestNo)
                        .refundReason("订单已取消，无需支付")
                        .fullRefund(true)
                        .refundAmount(message.getTotalAmount())
                        .orderCancelled(true)
                        .build();

                mqService.send(
                        OrderMQConstants.EXCHANGE_TOPIC_ORDER_REFUND_APPLY_TO_PAYMENT,
                        OrderMQConstants.ROUTING_ORDER_REFUND_APPLY_TO_PAYMENT,
                        refundMessage);
                return;
            }
            log.warn("[支付成功回调] 订单状态不允许支付回调落库，忽略 orderNo={}, orderStatus={}, payStatus={}",
                    orderNo, order.getOrderStatus(), order.getPaymentStatus());
            return;
        }

        // 5) 金额校验
        BigDecimal expected = order.getPayAmount();
        if (expected != null && message.getTotalAmount().compareTo(expected) != 0) {
            throw new IllegalStateException("[支付成功回调] 金额不一致 orderNo=" + orderNo
                    + ", expected=" + expected + ", actual=" + message.getTotalAmount());
        }

        // 6) 更新订单支付信息
        OrdersPaymentStatusEnum oldPayStatus = order.getPaymentStatus();
        OrderStatusEnum oldOrderStatus = order.getOrderStatus();

        order.setPaymentStatus(OrdersPaymentStatusEnum.PAID);
        order.setPaidAt(message.getPaymentAt());
        order.setTradeNo(message.getTradeNo());
        order.setOutTradeNo(message.getOutTradeNo());
        order.setPaymentType(message.getPaymentType());
        order.setPayAmount(message.getTotalAmount());
        order.setPaymentStatus(OrdersPaymentStatusEnum.PAID);

        // 支付成功后订单状态一般变为已支付/待履约
        // 例如：PENDING -> PAID / CONFIRMED / BOOKED 等
        order.setOrderStatus(OrderStatusEnum.PAID);

        ordersMapper.updateById(order);

        // 7) 记录状态日志（按你日志表字段来）
        OrderStatusLogs logEntity = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.PAY)
                .oldOrderStatus(oldOrderStatus)
                .newOrderStatus(order.getOrderStatus())
                .operatorType(OperatorTypeEnum.USER)
                .operatorId(order.getBuyerId())
                .operatorName("USER_" + order.getBuyerId())
                .remark(String.format("支付成功：tradeNo=%s, type=%s, amount=%s, paidAt=%s",
                        message.getTradeNo(),
                        message.getPaymentType(),
                        message.getTotalAmount(),
                        message.getPaymentAt()))
                .build();

        orderStatusLogsMapper.insert(logEntity);

        List<OrderItems> orderItems = orderItemsMapper.selectList(
                Wrappers.<OrderItems>lambdaQuery()
                        .eq(OrderItems::getOrderNo, orderNo));

        if (ObjectUtils.isEmpty(orderItems)) {
            throw new GloboxApplicationException(OrderCode.ORDER_ITEM_NOT_EXIST);
        }

        // 获取预约日期
        LocalDate bookingDate = orderItems.get(0).getBookingDate();

        OrderPaidMessage paidMessage = OrderPaidMessage.builder()
                .orderNo(orderNo)
                .userId(order.getBuyerId())
                .venueId(order.getSellerId())
                .build();
        // 8) 发送订单支付成功消息给商家
        if (order.getSellerType() == SellerTypeEnum.VENUE) {


            OrderActivities orderActivities = orderActivitiesMapper.selectOne(
                    Wrappers.<OrderActivities>lambdaQuery()
                            .eq(OrderActivities::getOrderNo, orderNo));
            if (ObjectUtils.isEmpty(orderActivities)) {
                // 不是活动订单
                paidMessage.setIsActivity(false);
                paidMessage.setRecordIds(
                        orderItems.stream()
                                .map(OrderItems::getRecordId)
                                .toList());
            } else {
                // 是活动订单
                paidMessage.setIsActivity(true);
                paidMessage.setRecordIds(List.of(orderActivities.getActivityId()));
            }

            // 发送订单已支付通知商家
            mqService.send(
                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT,
                    OrderMQConstants.ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT,
                    paidMessage);

        } else if (order.getSellerType().equals(SellerTypeEnum.COACH)) {
            // 9.2) 发送订单确认消息给教练

            paidMessage.setIsActivity(false);
            paidMessage.setRecordIds(
                    orderItems.stream()
                            .map(OrderItems::getRecordId)
                            .toList());

            mqService.send(
                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH,
                    OrderMQConstants.ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_COACH,
                    paidMessage);
            // 发送订单已支付通知给教练 - "您收到新的预约"
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                coachNotificationHelper.sendCoachAppointmentPaid(
                                        orderNo,
                                        order.getSellerId(),
                                        order.getBuyerId(),
                                        bookingDate.toString(),
                                        order.getPayAmount()
                                );
                            } catch (Exception e) {
                                log.error("[支付成功回调] 发送教练订单已支付通知失败, orderNo={}", orderNo, e);
                            }
                        }
                    });
        }


        // 找出最晚时间
        LocalTime localTime = orderItems
                .stream()
                .map(OrderItems::getEndTime)
                .max(LocalTime::compareTo)
                .orElse(LocalTime.of(23, 0));

        long delayMillis = LocalDateUtils.delayMillis(bookingDate, localTime);
        int delay = Math.toIntExact(delayMillis / 1000);

        // 10) 发送延迟消息订单完成
        OrderAutoCompleteMessage autoCompleteMessage = OrderAutoCompleteMessage.builder()
                .orderNo(orderNo)
                .retryCount(0)
                .build();
        mqService.sendDelay(
                OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_COMPLETE,
                OrderMQConstants.ROUTING_ORDER_AUTO_COMPLETE,
                autoCompleteMessage,
                delay);

        log.info("[支付成功回调] 处理完成 orderNo={}, payStatus {}->{} , orderStatus {}->{}",
                orderNo, oldPayStatus, order.getPaymentStatus(), oldOrderStatus, order.getOrderStatus());

    }
}
