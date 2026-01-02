package com.unlimited.sports.globox.common.constants;

/**
 * 订单相关 MQ 常量
 */
public class OrderMQConstants {

    /**
     * 订单创建失败或取消 → 通知场地解锁
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT =
            "exchange.topic.order.unlock-slot";
    public static final String ROUTING_ORDER_UNLOCK_SLOT =
            "routing.order.unlock-slot";
    public static final String QUEUE_ORDER_UNLOCK_SLOT_MERCHANT =
            "queue.order.unlock-slot.merchant";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_ORDER_UNLOCK_SLOT_MERCHANT_RETRY =
            "queue.order.unlock-slot.merchant.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_ORDER_UNLOCK_SLOT_RETRY_DLX =
            "exchange.topic.order.unlock-slot.retry.dlx";
    public static final String ROUTING_ORDER_UNLOCK_SLOT_RETRY =
            "routing.order.unlock-slot.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_ORDER_UNLOCK_SLOT_FINAL_DLX =
            "exchange.topic.order.unlock-slot.final.dlx";
    public static final String ROUTING_ORDER_UNLOCK_SLOT_FINAL =
            "routing.order.unlock-slot.final";
    public static final String QUEUE_ORDER_UNLOCK_SLOT_MERCHANT_DLQ =
            "queue.order.unlock-slot.merchant.dlq";


    /**
     * 订单创建成功事件
     * 无重试
     */
    public static final String EXCHANGE_TOPIC_ORDER_CREATED =
            "exchange.topic.order.created";
    public static final String ROUTING_ORDER_CREATED =
            "routing.order.created";
    public static final String QUEUE_ORDER_CREATED_MERCHANT =
            "queue.order.created.merchant";

    /**
     * Final-DLX
     */
    public static final String EXCHANGE_ORDER_CREATED_FINAL_DLX =
            "exchange.topic.order.created.final.dlx";
    public static final String ROUTING_ORDER_CREATED_FINAL =
            "routing.order.created.final";
    public static final String QUEUE_ORDER_CREATED_MERCHANT_DLQ =
            "queue.order.created.merchant.dlq";



    /**
     * 订单未支付自动关闭
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_ORDER_AUTO_CANCEL =
            "exchange.topic.order.auto-cancel";
    public static final String ROUTING_ORDER_AUTO_CANCEL =
            "routing.order.auto-cancel";
    public static final String QUEUE_ORDER_AUTO_CANCEL_ORDER =
            "queue.order.auto-cancel.order";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_ORDER_AUTO_CANCEL_ORDER_RETRY =
            "queue.order.auto-cancel.order.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_ORDER_AUTO_CANCEL_RETRY_DLX =
            "exchange.topic.order.auto-cancel.retry.dlx";
    public static final String ROUTING_ORDER_AUTO_CANCEL_RETRY =
            "routing.order.auto-cancel.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_ORDER_AUTO_CANCEL_FINAL_DLX =
            "exchange.topic.order.auto-cancel.final.dlx";
    public static final String ROUTING_ORDER_AUTO_CANCEL_FINAL =
            "routing.order.auto-cancel.final";
    public static final String QUEUE_ORDER_AUTO_CANCEL_ORDER_DLQ =
            "queue.order.auto-cancel.order.dlq";

}
