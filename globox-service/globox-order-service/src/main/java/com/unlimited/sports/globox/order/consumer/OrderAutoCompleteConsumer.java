package com.unlimited.sports.globox.order.consumer;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.enums.order.OperatorTypeEnum;
import com.unlimited.sports.globox.common.enums.order.OrderActionEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.lock.RedisLock;
import com.unlimited.sports.globox.common.message.order.OrderAutoCancelMessage;
import com.unlimited.sports.globox.common.message.order.OrderAutoCompleteMessage;
import com.unlimited.sports.globox.common.message.order.ProfitSharingMessage;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_AUTO_COMPLETE_FINAL,
            bizKey = "#message.orderNo",
            bizType = MQBizTypeEnum.ORDER_AUTO_COMPLETE
    )
    public void onMessage(
            OrderAutoCompleteMessage message,
            Channel channel,
            Message amqpMessage) {
        LocalDateTime now = LocalDateTime.now();

        message.incrementRetryCount();
        log.info("[订单自动完成] 收到订单自动完成消息 orderNo：{}", message.getOrderNo());

        Orders orders = ordersMapper.selectOne(
                Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getOrderNo, message.getOrderNo()));
        Assert.isNotEmpty(orders, OrderCode.ORDER_NOT_EXIST);

        OrderStatusEnum orderStatus = orders.getOrderStatus();
        log.info("[订单自动完成] 当前订单状态 orderNo：{}， orderStatus:{}", message.getOrderNo(), orderStatus);
        if (orderStatus.equals(OrderStatusEnum.PAID)
                || orderStatus.equals(OrderStatusEnum.CONFIRMED)
                || orderStatus.equals(OrderStatusEnum.PARTIALLY_REFUNDED)
                || orderStatus.equals(OrderStatusEnum.REFUND_REJECTED)
                || orderStatus.equals(OrderStatusEnum.REFUND_CANCELLED)) {
            // 已支付、已确认、部分退款、退款被拒绝、退款取消 标记为 已完成
            orders.setOrderStatus(OrderStatusEnum.COMPLETED);
            orders.setCompletedAt(now);
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

            // 如果属于教练订单，发送分账消息
            ProfitSharingMessage profitSharingMessage = new ProfitSharingMessage();
            BeanUtils.copyProperties(orders, profitSharingMessage);

            log.info("[订单自动完成] 订单:{} 状态已更新为完成:{}", orders.getOrderNo(), orders.getOrderNo());

        } else if (orderStatus.equals(OrderStatusEnum.REFUND_APPLYING)
                || orderStatus.equals(OrderStatusEnum.REFUNDING)) {
            if (message.getRetryCount() > 15) {
                log.error("[订单自动完成] orderNo：{} 第 {} 次尝试更新状态为完成但失败", orders.getOrderNo(), message.getRetryCount());
            } else if (message.getRetryCount() > 5) {
                log.warn("[订单自动完成] orderNo：{} 第 {} 次尝试更新状态为完成但失败", orders.getOrderNo(), message.getRetryCount());
            }

            // 正在退款申请、正在退款 发送一条半小时后的消息，等待订单状态变化
            int delay = 30 * 60;
            mqService.sendDelay(
                    OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_COMPLETE,
                    OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL,
                    message,
                    delay);
            log.info("[订单自动完成] 当前订单状态不能变为已完成，{}s 后重试，orderNo:{}, 当前 orderStatus:{}",delay, orders.getOrderNo(), orderStatus);
        } else if (orderStatus.equals(OrderStatusEnum.PENDING)
                || orderStatus.equals(OrderStatusEnum.COMPLETED)
                || orderStatus.equals(OrderStatusEnum.CANCELLED)
                || orderStatus.equals(OrderStatusEnum.REFUNDED)) {
            // 待支付 已完成 已取消 已全额退款 状态不做更改
            log.info("[订单自动完成] 当前订单状态不需要改变，orderNo:{}, orderStatus:{}", orders.getOrderNo(), orderStatus);
        } else {
            // 未知支付状态异常
            throw new GloboxApplicationException(OrderCode.ORDER_STATUS_NOT_EXIST);
        }

    }

}
