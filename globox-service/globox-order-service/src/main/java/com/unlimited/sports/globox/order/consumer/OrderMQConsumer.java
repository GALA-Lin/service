package com.unlimited.sports.globox.order.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.constants.MQConstants;
import com.unlimited.sports.globox.common.enums.order.OperatorTypeEnum;
import com.unlimited.sports.globox.common.enums.order.OrderActionEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.PaymentStatusEnum;
import com.unlimited.sports.globox.common.message.OrderAutoCancelMessage;
import com.unlimited.sports.globox.common.message.UnlockSlotMessage;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.model.order.entity.OrderStatusLogs;
import com.unlimited.sports.globox.model.order.entity.Orders;
import com.unlimited.sports.globox.order.mapper.OrderItemsMapper;
import com.unlimited.sports.globox.order.mapper.OrderStatusLogsMapper;
import com.unlimited.sports.globox.order.mapper.OrdersMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;


/**
 * 订单自动取消消费者，用于处理未支付订单的自动关闭逻辑。
 * 该类监听指定的消息队列，当接收到延迟消息时，根据订单编号查询并处理未支付订单。
 * 如果订单存在且状态为未支付，则将订单状态更改为已取消，并记录相应的日志信息。
 * 此外，通过数据库行锁确保并发安全，避免在处理过程中出现竞态条件。
 */
@Slf4j
@Component
@RabbitListener(
        queues = MQConstants.QUEUE_ORDER_AUTO_CANCEL_ORDER)
public class OrderMQConsumer {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderItemsMapper orderItemsMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private MQService mqService;

    /**
     * 延迟关闭未支付订单
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(
            OrderAutoCancelMessage message,
            Channel channel,
            Message amqpMessage) throws IOException {

        Long orderNo = message.getOrderNo();
        log.info("[订单自动关闭] 接收到延迟消息 orderNo={}", orderNo);

        try {
            // 1. 查询订单（加行锁）
            Orders order = ordersMapper.selectOne(
                    Wrappers.<Orders>lambdaQuery()
                            .eq(Orders::getOrderNo, orderNo)
                            .last("FOR UPDATE"));

            if (order == null) {
                log.warn("[订单自动关闭] 订单不存在 orderNo={}", orderNo);
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 2. 幂等判断
            if (order.getPaymentStatus() != PaymentStatusEnum.UNPAID ||
                    order.getOrderStatus() != OrderStatusEnum.PENDING) {

                log.info(
                        "[订单自动关闭] 订单无需处理 orderNo={}, status={}, payStatus={}",
                        orderNo,
                        order.getOrderStatus(),
                        order.getPaymentStatus());
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 3. 执行关闭订单
            order.setOrderStatus(OrderStatusEnum.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            ordersMapper.updateById(order);

            // 4. 写状态流转日志
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

            // 5. 发送解锁场地消息
            UnlockSlotMessage unlockMessage = UnlockSlotMessage.builder()
                    .userId(message.getUserId())
                    .recordIds(message.getSlotIds())
                    .bookingDate(message.getBookingDate())
                    .build();

            mqService.send(
                    MQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                    MQConstants.ROUTING_ORDER_UNLOCK_SLOT,
                    unlockMessage);

            /* ==================== 6. ACK ==================== */
            channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
            log.info("[订单自动关闭] 成功关闭 orderNo={}", orderNo);

        } catch (Exception e) {
            log.error("[订单自动关闭] 处理失败 orderNo={}", orderNo, e);

            // 失败重试（保证消息不丢）
            channel.basicNack(
                    amqpMessage.getMessageProperties().getDeliveryTag(),
                    false,
                    true);
        }
    }
}
