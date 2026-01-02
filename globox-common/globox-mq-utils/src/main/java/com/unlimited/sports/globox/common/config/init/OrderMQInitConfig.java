package com.unlimited.sports.globox.common.config.init;

import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


/**
 * 订单相关队列绑定
 */
@Configuration
public class OrderMQInitConfig {

    /**
     * 订单自动取消重试间隔
     * 默认 5000 ms
     */
    @Value( "${mq.consumer.retry.order-auto-cancel.retry-interval:5000}")
    private int autoCancelRetryInterval;

    /**
     * 取消锁场重试间隔
     * 默认 1000 ms
     */
    @Value( "${mq.consumer.retry.merchant-unlock-slot.retry-interval:1000}")
    private int unlockRetryInterval;

    /**
     * 订单未支付自动关闭（Auto Cancel）
     */
    // 主交换机
    @Bean
    public CustomExchange orderAutoCancelExchange() {
        Map<String, Object> args = new HashMap<>();
        // 关键：让延迟交换机表现为 topic 类型
        args.put("x-delayed-type", "topic");

        return new CustomExchange(
                OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_CANCEL,
                "x-delayed-message",
                true,
                false,
                args);
    }

    // Retry-DLX 交换机：主队列 reject 后进入 retry.queue
    @Bean
    public TopicExchange orderAutoCancelRetryDlxExchange() {
        return new TopicExchange(OrderMQConstants.EXCHANGE_ORDER_AUTO_CANCEL_RETRY_DLX, true, false);
    }

    // Final-DLX 交换机：超过最大次数后进入最终 DLQ（由你业务判定后投递）
    @Bean
    public TopicExchange orderAutoCancelFinalDlxExchange() {
        return new TopicExchange(OrderMQConstants.EXCHANGE_ORDER_AUTO_CANCEL_FINAL_DLX, true, false);
    }

    // 主队列：失败后 DLX 到 Retry-DLX（而不是直接进最终 DLQ）
    @Bean
    public Queue orderAutoCancelQueue() {
        return QueueBuilder
                .durable(OrderMQConstants.QUEUE_ORDER_AUTO_CANCEL_ORDER)
                .withArgument("x-dead-letter-exchange", OrderMQConstants.EXCHANGE_ORDER_AUTO_CANCEL_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL_RETRY)
                .build();
    }

    // Retry Queue：TTL 到期后 DLX 回主交换机 + 主 routingKey
    @Bean
    public Queue orderAutoCancelRetryQueue() {
        return QueueBuilder.durable(OrderMQConstants.QUEUE_ORDER_AUTO_CANCEL_ORDER_RETRY)
                .withArgument("x-message-ttl", autoCancelRetryInterval)
                .withArgument("x-dead-letter-exchange", OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_CANCEL)
                .withArgument("x-dead-letter-routing-key", OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL)
                .build();
    }

    // 最终 DLQ：超过最大次数后由业务投递到 Final-DLX
    @Bean
    public Queue orderAutoCancelDlq() {
        return QueueBuilder.durable(OrderMQConstants.QUEUE_ORDER_AUTO_CANCEL_ORDER_DLQ).build();
    }

    // 主交换机 -> 主队列
    @Bean
    public Binding bindOrderAutoCancelQueue() {
        return BindingBuilder.bind(orderAutoCancelQueue())
                .to(orderAutoCancelExchange())
                .with(OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL)
                .noargs();
    }

    // Retry-DLX -> retry.queue
    @Bean
    public Binding bindOrderAutoCancelRetryQueue() {
        return BindingBuilder.bind(orderAutoCancelRetryQueue())
                .to(orderAutoCancelRetryDlxExchange())
                .with(OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL_RETRY);
    }

    // Final-DLX -> 最终 DLQ
    @Bean
    public Binding bindOrderAutoCancelDlq() {
        return BindingBuilder.bind(orderAutoCancelDlq())
                .to(orderAutoCancelFinalDlxExchange())
                .with(OrderMQConstants.ROUTING_ORDER_AUTO_CANCEL_FINAL);
    }


    /**
     * 通知场地解锁（Unlock Slot）
     */
    @Bean
    public TopicExchange orderUnlockSlotExchange() {
        return new TopicExchange(OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT, true, false);
    }

    @Bean
    public TopicExchange orderUnlockSlotRetryDlxExchange() {
        return new TopicExchange(OrderMQConstants.EXCHANGE_ORDER_UNLOCK_SLOT_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange orderUnlockSlotFinalDlxExchange() {
        return new TopicExchange(OrderMQConstants.EXCHANGE_ORDER_UNLOCK_SLOT_FINAL_DLX, true, false);
    }

    @Bean
    public Queue orderUnlockSlotQueue() {
        return QueueBuilder.durable(OrderMQConstants.QUEUE_ORDER_UNLOCK_SLOT_MERCHANT)
                .withArgument("x-dead-letter-exchange", OrderMQConstants.EXCHANGE_ORDER_UNLOCK_SLOT_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT_RETRY)
                .build();
    }

    @Bean
    public Queue orderUnlockSlotRetryQueue() {
        return QueueBuilder.durable(OrderMQConstants.QUEUE_ORDER_UNLOCK_SLOT_MERCHANT_RETRY)
                // 并发冲突：建议短延迟（毫秒~秒）
                .withArgument("x-message-ttl", unlockRetryInterval)
                .withArgument("x-dead-letter-exchange", OrderMQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT)
                .withArgument("x-dead-letter-routing-key", OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT)
                .build();
    }

    @Bean
    public Queue orderUnlockSlotDlq() {
        return QueueBuilder.durable(OrderMQConstants.QUEUE_ORDER_UNLOCK_SLOT_MERCHANT_DLQ).build();
    }

    @Bean
    public Binding bindOrderUnlockSlotQueue() {
        return BindingBuilder.bind(orderUnlockSlotQueue())
                .to(orderUnlockSlotExchange())
                .with(OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT);
    }

    @Bean
    public Binding bindOrderUnlockSlotRetryQueue() {
        return BindingBuilder.bind(orderUnlockSlotRetryQueue())
                .to(orderUnlockSlotRetryDlxExchange())
                .with(OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT_RETRY);
    }

    @Bean
    public Binding bindOrderUnlockSlotDlq() {
        return BindingBuilder.bind(orderUnlockSlotDlq())
                .to(orderUnlockSlotFinalDlxExchange())
                .with(OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT_FINAL);
    }


    /**
     * 订单创建成功事件（Created）
     * 通常不需要 retry，失败直接进入 DLQ 可选
     */
    @Bean
    public TopicExchange orderCreatedExchange() {
        return new TopicExchange(OrderMQConstants.EXCHANGE_TOPIC_ORDER_CREATED, true, false);
    }

    @Bean
    public TopicExchange orderCreatedFinalDlxExchange() {
        return new TopicExchange(OrderMQConstants.EXCHANGE_ORDER_CREATED_FINAL_DLX, true, false);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(OrderMQConstants.QUEUE_ORDER_CREATED_MERCHANT)
                // created 失败直接进 DLQ，就保留这两行
                .withArgument("x-dead-letter-exchange", OrderMQConstants.EXCHANGE_ORDER_CREATED_FINAL_DLX)
                .withArgument("x-dead-letter-routing-key", OrderMQConstants.ROUTING_ORDER_CREATED_FINAL)
                .build();
    }

    @Bean
    public Queue orderCreatedDlq() {
        return QueueBuilder.durable(OrderMQConstants.QUEUE_ORDER_CREATED_MERCHANT_DLQ).build();
    }

    @Bean
    public Binding bindOrderCreatedQueue() {
        return BindingBuilder.bind(orderCreatedQueue())
                .to(orderCreatedExchange())
                .with(OrderMQConstants.ROUTING_ORDER_CREATED);
    }

    @Bean
    public Binding bindOrderCreatedDlq() {
        return BindingBuilder.bind(orderCreatedDlq())
                .to(orderCreatedFinalDlxExchange())
                .with(OrderMQConstants.ROUTING_ORDER_CREATED_FINAL);
    }
}