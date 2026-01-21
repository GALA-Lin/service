package com.unlimited.sports.globox.social.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unlimited.sports.globox.common.constants.RallyMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置类
 * 用于配置消息队列、交换机、绑定和序列化器
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 社交消息相关配置 ====================
    // 交换机名称
    public static final String SOCIAL_EXCHANGE = "social.exchange";

    // 队列名称
    public static final String MESSAGE_QUEUE = "social.message.queue";
    public static final String CONVERSATION_QUEUE = "social.conversation.queue";
    public static final String BATCH_MESSAGE_QUEUE = "social.batch.message.queue";

    // 路由键
    public static final String MESSAGE_ROUTING_KEY = "social.message.save";
    public static final String CONVERSATION_ROUTING_KEY = "social.conversation.update";
    public static final String BATCH_MESSAGE_ROUTING_KEY = "social.batch.message.save";


    // 死信路由键
    public static final String MESSAGE_DLX_ROUTING_KEY = "social.message.dlx";
    public static final String CONVERSATION_DLX_ROUTING_KEY = "social.conversation.dlx";
    public static final String BATCH_MESSAGE_DLX_ROUTING_KEY = "social.batch.message.dlx";

    /**
     * 声明交换机
     */
    @Bean
    public Exchange socialExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        // 使用普通direct交换机
        return new DirectExchange(SOCIAL_EXCHANGE, true, false, args);
    }

    /**
     * 声明消息保存队列
     */
    @Bean
    public Queue messageQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置死信交换机和路由键
        args.put("x-dead-letter-exchange", SOCIAL_EXCHANGE);
        args.put("x-dead-letter-routing-key", "social.message.dlx");
        // 设置消息TTL（可选）
        // args.put("x-message-ttl", 60000);
        return new Queue(MESSAGE_QUEUE, true, false, false, args);
    }

    /**
     * 声明会话更新队列
     */
    @Bean
    public Queue conversationQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SOCIAL_EXCHANGE);
        args.put("x-dead-letter-routing-key", "social.conversation.dlx");
        return new Queue(CONVERSATION_QUEUE, true, false, false, args);
    }

    /**
     * 声明批量消息队列
     */
    @Bean
    public Queue batchMessageQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", SOCIAL_EXCHANGE);
        args.put("x-dead-letter-routing-key", "social.batch.message.dlx");
        return new Queue(BATCH_MESSAGE_QUEUE, true, false, false, args);
    }

    /**
     * 绑定消息队列到交换机
     */
    @Bean
    public Binding messageQueueBinding() {
        return BindingBuilder.bind(messageQueue())
                .to(socialExchange())
                .with(MESSAGE_ROUTING_KEY)
                .noargs();
    }

    /**
     * 绑定会话队列到交换机
     */
    @Bean
    public Binding conversationQueueBinding() {
        return BindingBuilder.bind(conversationQueue())
                .to(socialExchange())
                .with(CONVERSATION_ROUTING_KEY)
                .noargs();
    }

    /**
     * 绑定批量消息队列到交换机
     */
    @Bean
    public Binding batchMessageQueueBinding() {
        return BindingBuilder.bind(batchMessageQueue())
                .to(socialExchange())
                .with(BATCH_MESSAGE_ROUTING_KEY)
                .noargs();
    }

    /**
     * 配置消息转换器，使用JSON格式
     * 支持Java 8日期时间类型
     */
    @Bean
    public MessageConverter messageConverter() {
        // 创建自定义的ObjectMapper，支持Java 8日期时间
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 创建Jackson2JsonMessageConverter并使用自定义ObjectMapper
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 配置RabbitTemplate，使用JSON转换器
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }


    /**
     * 配置监听容器工厂
     * 约球提醒队列与交换机的绑定
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        // 设置并发消费者数量
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(10);
        // 设置预取消息数量
        factory.setPrefetchCount(10);
        return factory;
    }

    // ==================== 约球提醒相关配置 ====================

    /**
     * 约球开始提醒队列
     */
    @Bean
    public Queue rallyStartingReminderQueue() {
        return new Queue(RallyMQConstants.QUEUE_RALLY_STARTING_REMINDER, true, false, false);
    }

    /**
     * 约球开始提醒重试队列
     */
    @Bean
    public Queue rallyStartingReminderRetryQueue() {
        return new Queue(RallyMQConstants.QUEUE_RALLY_STARTING_REMINDER_RETRY, true, false, false);
    }

    /**
     * 约球开始提醒死信队列
     */
    @Bean
    public Queue rallyStartingReminderDlq() {
        return new Queue(RallyMQConstants.QUEUE_RALLY_STARTING_REMINDER_DLQ, true, false, false);
    }

    /**
     * 约球提醒交换机
     */
    @Bean
    public TopicExchange rallyStartingReminderExchange() {
        return new TopicExchange(RallyMQConstants.EXCHANGE_TOPIC_RALLY_STARTING_REMINDER, true, false);
    }

    /**
     * 约球提醒队列与交换机的绑定
     */
    @Bean
    public Binding rallyStartingReminderBinding(Queue rallyStartingReminderQueue, TopicExchange rallyStartingReminderExchange) {
        return BindingBuilder.bind(rallyStartingReminderQueue)
                .to(rallyStartingReminderExchange)
                .with(RallyMQConstants.ROUTING_RALLY_STARTING_REMINDER);
    }




}
