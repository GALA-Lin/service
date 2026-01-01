package com.unlimited.sports.globox.common.constants;

/**
 * 消息队列常量类
 */
public class MQConstants {
    /**
     * 订单创建失败或取消，通知场地取消锁场
     */
    public static final String EXCHANGE_TOPIC_ORDER_UNLOCK_SLOT = "exchange.topic.order.unlock-slot";
    public static final String ROUTING_ORDER_UNLOCK_SLOT = "routing.order.unlock-slot";
    public static final String QUEUE_ORDER_UNLOCK_SLOT_MERCHANT = "queue.order.unlock-slot.merchant";

    /**
     * 订单创建成功事件
     */
    public static final String EXCHANGE_TOPIC_ORDER_CREATED = "exchange.topic.order.created";
    public static final String ROUTING_ORDER_CREATED = "routing.order.created";
    public static final String QUEUE_ORDER_CREATED_MERCHANT = "queue.order.created.merchant";


    /**
     * 订单未支付自动关闭事件
     */
    public static final String EXCHANGE_TOPIC_ORDER_AUTO_CANCEL = "exchange.topic.order.auto-cancel";
    public static final String ROUTING_ORDER_AUTO_CANCEL = "routing.order.auto-cancel";
    public static final String QUEUE_ORDER_AUTO_CANCEL_ORDER = "queue.order.auto-cancel.order";
}
