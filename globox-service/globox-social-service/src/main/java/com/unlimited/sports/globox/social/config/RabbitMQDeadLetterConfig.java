package com.unlimited.sports.globox.social.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ死信队列配置
 * 用于处理失败的消息
 */
@Configuration
public class RabbitMQDeadLetterConfig {

    // 死信队列名称
    public static final String MESSAGE_DLQ = "social.message.dlq";
    public static final String CONVERSATION_DLQ = "social.conversation.dlq";
    public static final String BATCH_MESSAGE_DLQ = "social.batch.message.dlq";
    
    // 死信交换机名称
    public static final String DLX_EXCHANGE = "social.dlx.exchange";
    
    // 死信路由键
    public static final String MESSAGE_DLX_ROUTING_KEY = "social.message.dlx";
    public static final String CONVERSATION_DLX_ROUTING_KEY = "social.conversation.dlx";
    public static final String BATCH_MESSAGE_DLX_ROUTING_KEY = "social.batch.message.dlx";

    /**
     * 声明死信交换机
     */
    @Bean
    public Exchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    /**
     * 声明消息死信队列
     */
    @Bean
    public Queue messageDeadLetterQueue() {
        return new Queue(MESSAGE_DLQ, true, false, false);
    }

    /**
     * 声明会话死信队列
     */
    @Bean
    public Queue conversationDeadLetterQueue() {
        return new Queue(CONVERSATION_DLQ, true, false, false);
    }

    /**
     * 声明批量消息死信队列
     */
    @Bean
    public Queue batchMessageDeadLetterQueue() {
        return new Queue(BATCH_MESSAGE_DLQ, true, false, false);
    }

    /**
     * 绑定消息死信队列
     */
    @Bean
    public Binding messageDeadLetterBinding() {
        return BindingBuilder.bind(messageDeadLetterQueue())
                .to(dlxExchange())
                .with(MESSAGE_DLX_ROUTING_KEY)
                .noargs();
    }

    /**
     * 绑定会话死信队列
     */
    @Bean
    public Binding conversationDeadLetterBinding() {
        return BindingBuilder.bind(conversationDeadLetterQueue())
                .to(dlxExchange())
                .with(CONVERSATION_DLX_ROUTING_KEY)
                .noargs();
    }

    /**
     * 绑定批量消息死信队列
     */
    @Bean
    public Binding batchMessageDeadLetterBinding() {
        return BindingBuilder.bind(batchMessageDeadLetterQueue())
                .to(dlxExchange())
                .with(BATCH_MESSAGE_DLX_ROUTING_KEY)
                .noargs();
    }
}
