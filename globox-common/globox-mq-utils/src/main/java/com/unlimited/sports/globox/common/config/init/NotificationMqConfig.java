package com.unlimited.sports.globox.common.config.init;

import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务MQ配置
 * 包含主队列、重试队列、死信队列的完整配置
 */
@Configuration
public class NotificationMqConfig {

    /**
     * 紧急通知重试间隔（默认1秒）
     */
    @Value("${mq.consumer.retry.notification.urgent.retry-interval:1000}")
    private int urgentRetryInterval;

    /**
     * 核心业务重试间隔（默认3秒）
     */
    @Value("${mq.consumer.retry.notification.core.retry-interval:3000}")
    private int coreRetryInterval;

    /**
     * 系统消息重试间隔（默认5秒）
     */
    @Value("${mq.consumer.retry.notification.system.retry-interval:5000}")
    private int systemRetryInterval;

    /**
     * 设备激活重试间隔（默认2秒）
     */
    @Value("${mq.consumer.retry.notification.device.activation.retry-interval:2000}")
    private int deviceActivationRetryInterval;

    /**
     * 通知服务统一Topic Exchange
     */
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(
                NotificationMQConstants.EXCHANGE_TOPIC_NOTIFICATION,
                true,
                false);
    }

    /**
     * 紧急通知重试链路
     */
    // Retry-DLX交换机
    @Bean
    public TopicExchange notificationUrgentRetryDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_NOTIFICATION_URGENT_RETRY_DLX, true, false);
    }

    // Final-DLX交换机
    @Bean
    public TopicExchange notificationUrgentFinalDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_NOTIFICATION_URGENT_FINAL_DLX, true, false);
    }

    // 主队列（失败后进入Retry-DLX）
    @Bean
    public Queue notificationUrgentQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_URGENT)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_NOTIFICATION_URGENT_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_NOTIFICATION_URGENT_RETRY)
                .build();
    }

    // 重试队列（TTL到期后回到主交换机）
    @Bean
    public Queue notificationUrgentRetryQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_URGENT_RETRY)
                .withArgument("x-message-ttl", urgentRetryInterval)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_TOPIC_NOTIFICATION)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_NOTIFICATION_URGENT)
                .build();
    }

    // 主交换机 -> 主队列
    @Bean
    public Binding notificationUrgentBinding(
            Queue notificationUrgentQueue,
            TopicExchange notificationExchange) {
        return BindingBuilder
                .bind(notificationUrgentQueue)
                .to(notificationExchange)
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_URGENT);
    }

    // Retry-DLX -> 重试队列
    @Bean
    public Binding notificationUrgentRetryBinding() {
        return BindingBuilder
                .bind(notificationUrgentRetryQueue())
                .to(notificationUrgentRetryDlxExchange())
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_URGENT_RETRY);
    }

    // Final-DLX -> 统一死信队列
    @Bean
    public Binding notificationUrgentDlqBinding() {
        return BindingBuilder
                .bind(notificationDlqQueue())
                .to(notificationUrgentFinalDlxExchange())
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_URGENT_FINAL);
    }

    /**
     * 核心业务重试链路
     */
    @Bean
    public TopicExchange notificationCoreRetryDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_NOTIFICATION_CORE_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange notificationCoreFinalDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_NOTIFICATION_CORE_FINAL_DLX, true, false);
    }

    @Bean
    public Queue notificationCoreQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_CORE)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_NOTIFICATION_CORE_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_NOTIFICATION_CORE_RETRY)
                .build();
    }

    @Bean
    public Queue notificationCoreRetryQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_CORE_RETRY)
                .withArgument("x-message-ttl", coreRetryInterval)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_TOPIC_NOTIFICATION)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_NOTIFICATION_CORE)
                .build();
    }

    @Bean
    public Binding notificationCoreBinding(
            Queue notificationCoreQueue,
            TopicExchange notificationExchange) {
        return BindingBuilder
                .bind(notificationCoreQueue)
                .to(notificationExchange)
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_CORE);
    }

    @Bean
    public Binding notificationCoreRetryBinding() {
        return BindingBuilder
                .bind(notificationCoreRetryQueue())
                .to(notificationCoreRetryDlxExchange())
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_CORE_RETRY);
    }

    @Bean
    public Binding notificationCoreDlqBinding() {
        return BindingBuilder
                .bind(notificationDlqQueue())
                .to(notificationCoreFinalDlxExchange())
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_CORE_FINAL);
    }

    /**
     * 系统消息重试链路
     */
    @Bean
    public TopicExchange notificationSystemRetryDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_NOTIFICATION_SYSTEM_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange notificationSystemFinalDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_NOTIFICATION_SYSTEM_FINAL_DLX, true, false);
    }

    @Bean
    public Queue notificationSystemQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_SYSTEM)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_NOTIFICATION_SYSTEM_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_NOTIFICATION_SYSTEM_RETRY)
                .build();
    }

    @Bean
    public Queue notificationSystemRetryQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_SYSTEM_RETRY)
                .withArgument("x-message-ttl", systemRetryInterval)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_TOPIC_NOTIFICATION)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_NOTIFICATION_SYSTEM)
                .build();
    }

    @Bean
    public Binding notificationSystemBinding(
            Queue notificationSystemQueue,
            TopicExchange notificationExchange) {
        return BindingBuilder
                .bind(notificationSystemQueue)
                .to(notificationExchange)
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_SYSTEM);
    }

    @Bean
    public Binding notificationSystemRetryBinding() {
        return BindingBuilder
                .bind(notificationSystemRetryQueue())
                .to(notificationSystemRetryDlxExchange())
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_SYSTEM_RETRY);
    }

    @Bean
    public Binding notificationSystemDlqBinding() {
        return BindingBuilder
                .bind(notificationDlqQueue())
                .to(notificationSystemFinalDlxExchange())
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_SYSTEM_FINAL);
    }

    /**
     * 设备激活重试链路
     */
    @Bean
    public TopicExchange deviceActivationRetryDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_DEVICE_ACTIVATION_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange deviceActivationFinalDlxExchange() {
        return new TopicExchange(NotificationMQConstants.EXCHANGE_DEVICE_ACTIVATION_FINAL_DLX, true, false);
    }

    @Bean
    public Queue deviceActivationQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_DEVICE_ACTIVATION)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_DEVICE_ACTIVATION_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_DEVICE_ACTIVATION_RETRY)
                .build();
    }

    @Bean
    public Queue deviceActivationRetryQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_DEVICE_ACTIVATION_RETRY)
                .withArgument("x-message-ttl", deviceActivationRetryInterval)
                .withArgument("x-dead-letter-exchange", NotificationMQConstants.EXCHANGE_TOPIC_NOTIFICATION)
                .withArgument("x-dead-letter-routing-key", NotificationMQConstants.ROUTING_DEVICE_ACTIVATION)
                .build();
    }

    @Bean
    public Queue deviceActivationDlq() {
        return QueueBuilder.durable(NotificationMQConstants.QUEUE_DEVICE_ACTIVATION_DLQ).build();
    }

    @Bean
    public Binding deviceActivationBinding(
            Queue deviceActivationQueue,
            TopicExchange notificationExchange) {
        return BindingBuilder
                .bind(deviceActivationQueue)
                .to(notificationExchange)
                .with(NotificationMQConstants.ROUTING_DEVICE_ACTIVATION);
    }

    @Bean
    public Binding deviceActivationRetryBinding() {
        return BindingBuilder
                .bind(deviceActivationRetryQueue())
                .to(deviceActivationRetryDlxExchange())
                .with(NotificationMQConstants.ROUTING_DEVICE_ACTIVATION_RETRY);
    }

    @Bean
    public Binding deviceActivationDlqBinding() {
        return BindingBuilder
                .bind(notificationDlqQueue())
                .to(deviceActivationFinalDlxExchange())
                .with(NotificationMQConstants.ROUTING_DEVICE_ACTIVATION_FINAL);
    }

    /**
     * 统一死信队列：处理所有失败消息
     * 4个业务的Final-DLX都绑定到这个队列
     */
    @Bean
    public DirectExchange notificationDlqExchange() {
        return new DirectExchange(
                NotificationMQConstants.EXCHANGE_DLQ_NOTIFICATION,
                true,
                false);
    }

    @Bean
    public Queue notificationDlqQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_DLQ_NOTIFICATION)
                .build();
    }

    @Bean
    public Binding notificationDlqBinding(
            Queue notificationDlqQueue,
            DirectExchange notificationDlqExchange) {
        return BindingBuilder
                .bind(notificationDlqQueue)
                .to(notificationDlqExchange)
                .with(NotificationMQConstants.ROUTING_DLQ_NOTIFICATION);
    }
}
