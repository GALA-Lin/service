package com.unlimited.sports.globox.common.config.init;

import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付相关队列绑定
 */
@Configuration
public class PaymentMQInitConfig {
    /**
     * 支付成功事件重试间隔
     * 默认 1000 ms
     */
    @Value("${mq.consumer.retry.payment-success.retry-interval:1000}")
    private int paymentSuccessRetryInterval;

    /**
     * 支付取消事件重试间隔
     * 默认 1000 ms
     */
    @Value("${mq.consumer.retry.payment-cancel.retry-interval:1000}")
    private int paymentCancelRetryInterval;


    /**
     * 支付成功（Pay Success）
     */
    @Bean
    public TopicExchange paymentSuccessExchange() {
        return new TopicExchange(PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_SUCCESS, true, false);
    }

    @Bean
    public TopicExchange paymentSuccessRetryDlxExchange() {
        return new TopicExchange(PaymentMQConstants.EXCHANGE_PAYMENT_SUCCESS_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange paymentSuccessFinalDlxExchange() {
        return new TopicExchange(PaymentMQConstants.EXCHANGE_PAYMENT_SUCCESS_FINAL_DLX, true, false);
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return QueueBuilder
                .durable(PaymentMQConstants.QUEUE_PAYMENT_SUCCESS_ORDER)
                .withArgument("x-dead-letter-exchange", PaymentMQConstants.EXCHANGE_PAYMENT_SUCCESS_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", PaymentMQConstants.ROUTING_PAYMENT_SUCCESS_RETRY)
                .build();
    }

    @Bean
    public Queue paymentSuccessRetryQueue() {
        return QueueBuilder.durable(PaymentMQConstants.QUEUE_PAYMENT_SUCCESS_ORDER_RETRY)
                .withArgument("x-message-ttl", paymentSuccessRetryInterval)
                .withArgument("x-dead-letter-exchange", PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_SUCCESS)
                .withArgument("x-dead-letter-routing-key", PaymentMQConstants.ROUTING_PAYMENT_SUCCESS)
                .build();
    }

    @Bean
    public Queue paymentSuccessDlq() {
        return QueueBuilder.durable(PaymentMQConstants.QUEUE_PAYMENT_SUCCESS_ORDER_DLQ).build();
    }

    @Bean
    public Binding bindPaymentSuccessQueue() {
        return BindingBuilder.bind(paymentSuccessQueue())
                .to(paymentSuccessExchange())
                .with(PaymentMQConstants.ROUTING_PAYMENT_SUCCESS);
    }

    @Bean
    public Binding bindPaymentSuccessRetryQueue() {
        return BindingBuilder.bind(paymentSuccessRetryQueue())
                .to(paymentSuccessRetryDlxExchange())
                .with(PaymentMQConstants.ROUTING_PAYMENT_SUCCESS_RETRY);
    }

    @Bean
    public Binding bindPaymentSuccessDlq() {
        return BindingBuilder.bind(paymentSuccessDlq())
                .to(paymentSuccessFinalDlxExchange())
                .with(PaymentMQConstants.ROUTING_PAYMENT_SUCCESS_FINAL);
    }


    /**
     * 支付取消（Pay Cancel）
     */
    @Bean
    public TopicExchange paymentCancelExchange() {
        return new TopicExchange(PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_CANCEL, true, false);
    }

    @Bean
    public TopicExchange paymentCancelRetryDlxExchange() {
        return new TopicExchange(PaymentMQConstants.EXCHANGE_PAYMENT_CANCEL_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange paymentCancelFinalDlxExchange() {
        return new TopicExchange(PaymentMQConstants.EXCHANGE_PAYMENT_CANCEL_FINAL_DLX, true, false);
    }

    @Bean
    public Queue paymentCancelQueue() {
        return QueueBuilder
                .durable(PaymentMQConstants.QUEUE_PAYMENT_CANCEL_ORDER)
                .withArgument("x-dead-letter-exchange", PaymentMQConstants.EXCHANGE_PAYMENT_CANCEL_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", PaymentMQConstants.ROUTING_PAYMENT_CANCEL_RETRY)
                .build();
    }

    @Bean
    public Queue paymentCancelRetryQueue() {
        return QueueBuilder.durable(PaymentMQConstants.QUEUE_PAYMENT_CANCEL_ORDER_RETRY)
                .withArgument("x-message-ttl", paymentCancelRetryInterval)
                .withArgument("x-dead-letter-exchange", PaymentMQConstants.EXCHANGE_TOPIC_PAYMENT_CANCEL)
                .withArgument("x-dead-letter-routing-key", PaymentMQConstants.ROUTING_PAYMENT_CANCEL)
                .build();
    }

    @Bean
    public Queue paymentCancelDlq() {
        return QueueBuilder.durable(PaymentMQConstants.QUEUE_PAYMENT_CANCEL_ORDER_DLQ).build();
    }

    @Bean
    public Binding bindPaymentCancelQueue() {
        return BindingBuilder.bind(paymentCancelQueue())
                .to(paymentCancelExchange())
                .with(PaymentMQConstants.ROUTING_PAYMENT_CANCEL);
    }

    @Bean
    public Binding bindPaymentCancelRetryQueue() {
        return BindingBuilder.bind(paymentCancelRetryQueue())
                .to(paymentCancelRetryDlxExchange())
                .with(PaymentMQConstants.ROUTING_PAYMENT_CANCEL_RETRY);
    }

    @Bean
    public Binding bindPaymentCancelDlq() {
        return BindingBuilder.bind(paymentCancelDlq())
                .to(paymentCancelFinalDlxExchange())
                .with(PaymentMQConstants.ROUTING_PAYMENT_CANCEL_FINAL);
    }
}
