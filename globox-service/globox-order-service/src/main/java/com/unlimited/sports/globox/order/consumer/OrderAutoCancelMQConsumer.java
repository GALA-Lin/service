package com.unlimited.sports.globox.order.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.order.*;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.order.OrderAutoCancelMessage;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.model.order.entity.OrderStatusLogs;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.constants.RedisConsts;
import com.unlimited.sports.globox.order.mapper.OrderStatusLogsMapper;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


/**
 * 订单自动取消消费者，用于处理未支付订单的自动关闭逻辑。
 * 该类监听指定的消息队列，当接收到延迟消息时，根据订单编号查询并处理未支付订单。
 * 如果订单存在且状态为未支付，则将订单状态更改为已取消，并记录相应的日志信息。
 * 此外，通过数据库行锁确保并发安全，避免在处理过程中出现竞态条件。
 */
@Slf4j
@Component
public class OrderAutoCancelMQConsumer {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private MQService mqService;

    /**
     * 延迟关闭未支付订单
     */
    @RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_AUTO_CANCEL_ORDER)
    @Transactional(rollbackFor = Exception.class)
    @RedisLock(value = "#message.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_AUTO_CANCEL_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL_FINAL)
    public void onMessage(
            OrderAutoCancelMessage message,
            Channel channel,
            Message amqpMessage){

        Long orderNo = message.getOrderNo();
        log.info("[订单自动关闭] 接收到消息 orderNo={}", orderNo);

        Orders order = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, orderNo)
                        .last("FOR UPDATE"));

        if (order == null) {
            log.warn("[订单自动关闭] 订单不存在 orderNo={}", orderNo);
            return;
        }

        if (order.getPaymentStatus() != OrdersPaymentStatusEnum.UNPAID ||
                order.getOrderStatus() != OrderStatusEnum.PENDING) {

            log.info("[订单自动关闭] 订单无需处理 orderNo={}, status={}, payStatus={}",
                    orderNo, order.getOrderStatus(), order.getPaymentStatus());
            return;
        }

        order.setOrderStatus(OrderStatusEnum.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        ordersMapper.updateById(order);

        OrderStatusLogs logEntity = OrderStatusLogs.builder()
                .orderNo(orderNo)
                .orderId(order.getId())
                .orderItemId(null)
                .action(OrderActionEnum.CANCEL)
                .oldOrderStatus(OrderStatusEnum.PENDING)
                .newOrderStatus(OrderStatusEnum.CANCELLED)
                .operatorType(OperatorTypeEnum.SYSTEM)
                .operatorId(null)
                .operatorName(OperatorTypeEnum.SYSTEM.getOperatorTypeName())
                .remark("订单超时未支付，系统自动关闭")
                .build();
        orderStatusLogsMapper.insert(logEntity);

        UnlockSlotMessage unlockMessage = UnlockSlotMessage.builder()
                .userId(message.getUserId())
                .operatorType(OperatorTypeEnum.SYSTEM)
                .recordIds(message.getRecordIds())
                .isActivity(order.getActivity())
                .bookingDate(message.getBookingDate())
                .build();

        if (message.getSellerType().equals(SellerTypeEnum.VENUE)) {
            mqService.send(
                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                    OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                    unlockMessage);
        } else if (message.getSellerType().equals(SellerTypeEnum.COACH)){
            mqService.send(
                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_COACH_SLOT,
                    OrderMQConstants.ROUTING_ORDER_UNLOCK_COACH_SLOT,
                    message);
        } else {
            throw new GloboxApplicationException(OrderCode.ORDER_SELLER_TYPE_NOT_EXIST);
        }

        log.info("[订单自动关闭] 成功关闭 orderNo={}", orderNo);
    }
}
