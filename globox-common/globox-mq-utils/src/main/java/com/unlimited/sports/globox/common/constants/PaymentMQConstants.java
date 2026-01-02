package com.unlimited.sports.globox.common.constants;

/**
 * 支付模块队列常量
 */
public class PaymentMQConstants {

    /**
     * 支付成功 → 通知订单服务（更新订单为已支付等）
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_PAYMENT_SUCCESS =
            "exchange.topic.payment.pay-success";
    public static final String ROUTING_PAYMENT_SUCCESS =
            "routing.payment.pay-success";
    public static final String QUEUE_PAYMENT_SUCCESS_ORDER =
            "queue.payment.pay-success.order";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_PAYMENT_SUCCESS_ORDER_RETRY =
            "queue.payment.pay-success.order.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_PAYMENT_SUCCESS_RETRY_DLX =
            "exchange.topic.payment.pay-success.retry.dlx";
    public static final String ROUTING_PAYMENT_SUCCESS_RETRY =
            "routing.payment.pay-success.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_PAYMENT_SUCCESS_FINAL_DLX =
            "exchange.topic.payment.pay-success.final.dlx";
    public static final String ROUTING_PAYMENT_SUCCESS_FINAL =
            "routing.payment.pay-success.final";
    public static final String QUEUE_PAYMENT_SUCCESS_ORDER_DLQ =
            "queue.payment.pay-success.order.dlq";


    /**
     * 支付取消/失败 → 通知订单服务（关闭订单/释放资源等）
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_PAYMENT_CANCEL =
            "exchange.topic.payment.pay-cancel";
    public static final String ROUTING_PAYMENT_CANCEL =
            "routing.payment.pay-cancel";
    public static final String QUEUE_PAYMENT_CANCEL_ORDER =
            "queue.payment.pay-cancel.order";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_PAYMENT_CANCEL_ORDER_RETRY =
            "queue.payment.pay-cancel.order.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_PAYMENT_CANCEL_RETRY_DLX =
            "exchange.topic.payment.pay-cancel.retry.dlx";
    public static final String ROUTING_PAYMENT_CANCEL_RETRY =
            "routing.payment.pay-cancel.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_PAYMENT_CANCEL_FINAL_DLX =
            "exchange.topic.payment.pay-cancel.final.dlx";
    public static final String ROUTING_PAYMENT_CANCEL_FINAL =
            "routing.payment.pay-cancel.final";
    public static final String QUEUE_PAYMENT_CANCEL_ORDER_DLQ =
            "queue.payment.pay-cancel.order.dlq";
}
