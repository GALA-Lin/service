package com.unlimited.sports.globox.order.config;

import com.unlimited.sports.globox.common.constants.MQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderEventMqConfig {

    /**
     * 订单创建成功事件 Exchange
     */
    @Bean
    public TopicExchange orderCreatedExchange() {
        return new TopicExchange(
                MQConstants.EXCHANGE_TOPIC_ORDER_CREATED,
                true,
                false);
    }

    /**
     * 订单失败 / 取消，解锁场地 Exchange
     */
    @Bean
    public TopicExchange orderUnlockSlotExchange() {
        return new TopicExchange(
                MQConstants.EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT,
                true,
                false);
    }

    /**
     * 订单未支付自动取消 Exchange
     */
    @Bean
    public TopicExchange orderAutoCancelExchange() {
        return new TopicExchange(
                MQConstants.EXCHANGE_TOPIC_ORDER_AUTO_CANCEL,
                true,
                false);
    }

    /**
     * 商家服务：订单创建成功
     */
    @Bean
    public Queue orderCreatedMerchantQueue() {
        return QueueBuilder
                .durable(MQConstants.QUEUE_ORDER_CREATED_MERCHANT)
                .build();
    }

    /**
     * 商家服务：订单失败 / 取消 → 解锁场地
     */
    @Bean
    public Queue orderUnlockSlotMerchantQueue() {
        return QueueBuilder
                .durable(MQConstants.QUEUE_ORDER_UNLOCK_SLOT_MERCHANT)
                .build();
    }

    /**
     * 订单服务：订单未支付自动关闭
     */
    @Bean
    public Queue orderAutoCancelOrderQueue() {
        return QueueBuilder
                .durable(MQConstants.QUEUE_ORDER_AUTO_CANCEL_ORDER)
                .build();
    }

    /**
     * 订单创建成功 → 商家服务
     */
    @Bean
    public Binding orderCreatedMerchantBinding(
            Queue orderCreatedMerchantQueue,
            TopicExchange orderCreatedExchange) {
        return BindingBuilder
                .bind(orderCreatedMerchantQueue)
                .to(orderCreatedExchange)
                .with(MQConstants.ROUTING_ORDER_CREATED);
    }

    /**
     * 订单失败 / 取消 → 解锁场地（商家服务）
     */
    @Bean
    public Binding orderUnlockSlotMerchantBinding(
            Queue orderUnlockSlotMerchantQueue,
            TopicExchange orderUnlockSlotExchange) {
        return BindingBuilder
                .bind(orderUnlockSlotMerchantQueue)
                .to(orderUnlockSlotExchange)
                .with(MQConstants.ROUTING_ORDER_UNLOCK_SLOT);
    }

    /**
     * 订单未支付 → 自动取消（订单服务）
     */
    @Bean
    public Binding orderAutoCancelOrderBinding(
            Queue orderAutoCancelOrderQueue,
            TopicExchange orderAutoCancelExchange) {
        return BindingBuilder
                .bind(orderAutoCancelOrderQueue)
                .to(orderAutoCancelExchange)
                .with(MQConstants.ROUTING_ORDER_AUTO_CANCEL);
    }
}