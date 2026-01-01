package com.unlimited.sports.globox.common.constants;

/**
 * 通知服务消息队列常量类

 */
public class NotificationMQConstants {

    /**
     * 通知服务统一Topic Exchange
     */
    public static final String EXCHANGE_TOPIC_NOTIFICATION = "exchange.topic.notification";

    /**
     * 高优先级队列：紧急通知（如账户被锁定、异常告警等）
     */
    public static final String ROUTING_NOTIFICATION_URGENT = "notification.urgent";
    public static final String QUEUE_NOTIFICATION_URGENT = "queue.notification.urgent";

    /**
     * 核心业务队列：订单、预约、支付通知
     */
    public static final String ROUTING_NOTIFICATION_CORE = "notification.core";
    public static final String QUEUE_NOTIFICATION_CORE = "queue.notification.core";

    /**
     * 系统消息队列：系统公告、营销推送等低优先级消息
     */
    public static final String ROUTING_NOTIFICATION_SYSTEM = "notification.system";
    public static final String QUEUE_NOTIFICATION_SYSTEM = "queue.notification.system";

    /**
     * 死信队列：处理失败消息
     */
    public static final String EXCHANGE_DLQ_NOTIFICATION = "exchange.dlq.notification";
    public static final String ROUTING_DLQ_NOTIFICATION = "routing.dlq.notification";
    public static final String QUEUE_DLQ_NOTIFICATION = "queue.dlq.notification";

    /**
     * 设备激活队列：用户登录时同步设备Token到通知服务
     */
    public static final String ROUTING_DEVICE_ACTIVATION = "device.activation";
    public static final String QUEUE_DEVICE_ACTIVATION = "queue.device.activation";
}
