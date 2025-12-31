package com.unlimited.sports.globox.notification.config;

import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 通知服务MQ配置
 */
@Configuration
public class NotificationMqConfig {

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
     * 高优先级队列：紧急通知（如账户被锁定、异常告警等）
     */
    @Bean
    public Queue notificationUrgentQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_URGENT)
                .deadLetterExchange(NotificationMQConstants.EXCHANGE_DLQ_NOTIFICATION)
                .deadLetterRoutingKey(NotificationMQConstants.ROUTING_DLQ_NOTIFICATION)
                .build();
    }

    @Bean
    public Binding notificationUrgentBinding(
            Queue notificationUrgentQueue,
            TopicExchange notificationExchange) {
        return BindingBuilder
                .bind(notificationUrgentQueue)
                .to(notificationExchange)
                .with(NotificationMQConstants.ROUTING_NOTIFICATION_URGENT);
    }

    /**
     * 核心业务队列：订单、预约、支付通知
     */
    @Bean
    public Queue notificationCoreQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_CORE)
                .deadLetterExchange(NotificationMQConstants.EXCHANGE_DLQ_NOTIFICATION)
                .deadLetterRoutingKey(NotificationMQConstants.ROUTING_DLQ_NOTIFICATION)
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

    /**
     * 系统消息队列：系统公告、营销推送等低优先级消息
     */
    @Bean
    public Queue notificationSystemQueue() {
        return QueueBuilder
                .durable(NotificationMQConstants.QUEUE_NOTIFICATION_SYSTEM)
                .deadLetterExchange(NotificationMQConstants.EXCHANGE_DLQ_NOTIFICATION)
                .deadLetterRoutingKey(NotificationMQConstants.ROUTING_DLQ_NOTIFICATION)
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

    /**
     * 死信队列：处理失败消息
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
