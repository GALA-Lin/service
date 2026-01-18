package com.unlimited.sports.globox.order.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.order.OperatorTypeEnum;
import com.unlimited.sports.globox.common.enums.order.OrderActionEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.order.OrderAutoCancelMessage;
import com.unlimited.sports.globox.common.result.OrderCode;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.Assert;
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
import org.springframework.util.ObjectUtils;

/**
 * 订单定时完成 消费者
 */
@Slf4j
@Component
public class OrderAutoCompleteConsumer {

    @Autowired
    private OrdersMapper ordersMapper;

    @Autowired
    private OrderStatusLogsMapper orderStatusLogsMapper;

    @Autowired
    private MQService mqService;

    /**
     * 订单定时完成
     */
    @RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_AUTO_COMPLETE_ORDER)
    @Transactional(rollbackFor = Exception.class)
    @RedisLock(value = "#message.orderNo", prefix = RedisConsts.ORDER_LOCK_KEY_PREFIX)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_AUTO_COMPLETE_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_AUTO_COMPLETE_FINAL)
    public void onMessage(
            OrderAutoCancelMessage message,
            Channel channel,
            Message amqpMessage) {

        Orders orders = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, message.getOrderNo()));
        Assert.isNotEmpty(orders, OrderCode.ORDER_NOT_EXIST);

        OrderStatusEnum orderStatus = orders.getOrderStatus();
        if (orderStatus.equals(OrderStatusEnum.PAID)
                || orderStatus.equals(OrderStatusEnum.CONFIRMED)
                || orderStatus.equals(OrderStatusEnum.PARTIALLY_REFUNDED)
                || orderStatus.equals(OrderStatusEnum.REFUND_REJECTED)
                || orderStatus.equals(OrderStatusEnum.REFUND_CANCELLED)) {
            // 已支付、已确认、部分退款、退款被拒绝、退款取消 标记为 已完成
            orders.setOrderStatus(OrderStatusEnum.COMPLETED);
            ordersMapper.updateById(orders);

            OrderStatusLogs orderLog = OrderStatusLogs.builder()
                    .orderNo(orders.getOrderNo())
                    .orderId(orders.getId())
                    .orderItemId(null)
                    .action(OrderActionEnum.COMPLETE)
                    .oldOrderStatus(orderStatus)
                    .newOrderStatus(OrderStatusEnum.COMPLETED)
                    .operatorType(OperatorTypeEnum.SYSTEM)
                    .operatorId(null)
                    .operatorName(OperatorTypeEnum.SYSTEM.getOperatorTypeName())
                    .remark("订单已自动完成")
                    .build();
            orderStatusLogsMapper.insert(orderLog);
        } else if (orderStatus.equals(OrderStatusEnum.REFUND_APPLYING)
                || orderStatus.equals(OrderStatusEnum.REFUNDING)) {
            // 正在退款申请、正在退款 发送一条半小时后的消息，等待订单状态变化
            int delay = 30 * 60;
            mqService.sendDelay(
                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_COMPLETE,
                    OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL,
                    message,
                    delay);
        } else if (orderStatus.equals(OrderStatusEnum.PENDING)
                || orderStatus.equals(OrderStatusEnum.COMPLETED)
                || orderStatus.equals(OrderStatusEnum.CANCELLED)
                || orderStatus.equals(OrderStatusEnum.REFUNDED)) {
            // 待支付 已完成 已取消 已全额退款 状态不做更改
        } else {
            // 未知支付状态异常
            throw new GloboxApplicationException(OrderCode.ORDER_STATUS_NOT_EXIST);
        }

    }

}
