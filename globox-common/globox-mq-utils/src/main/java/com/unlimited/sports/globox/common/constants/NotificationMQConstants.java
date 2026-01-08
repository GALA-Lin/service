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

    /**
     * 紧急通知重试链路
      */
    public static final String QUEUE_NOTIFICATION_URGENT_RETRY = "queue.notification.urgent.retry";
    public static final String EXCHANGE_NOTIFICATION_URGENT_RETRY_DLX = "exchange.notification.urgent.retry.dlx";
    public static final String ROUTING_NOTIFICATION_URGENT_RETRY = "notification.urgent.retry";
    public static final String EXCHANGE_NOTIFICATION_URGENT_FINAL_DLX = "exchange.notification.urgent.final.dlx";
    public static final String ROUTING_NOTIFICATION_URGENT_FINAL = "notification.urgent.final";

    /**
     * 核心业务重试链路
     */
    public static final String QUEUE_NOTIFICATION_CORE_RETRY = "queue.notification.core.retry";
    public static final String EXCHANGE_NOTIFICATION_CORE_RETRY_DLX = "exchange.notification.core.retry.dlx";
    public static final String ROUTING_NOTIFICATION_CORE_RETRY = "notification.core.retry";
    public static final String EXCHANGE_NOTIFICATION_CORE_FINAL_DLX = "exchange.notification.core.final.dlx";
    public static final String ROUTING_NOTIFICATION_CORE_FINAL = "notification.core.final";

    /**
     * 系统消息重试链路
     */
    public static final String QUEUE_NOTIFICATION_SYSTEM_RETRY = "queue.notification.system.retry";
    public static final String EXCHANGE_NOTIFICATION_SYSTEM_RETRY_DLX = "exchange.notification.system.retry.dlx";
    public static final String ROUTING_NOTIFICATION_SYSTEM_RETRY = "notification.system.retry";
    public static final String EXCHANGE_NOTIFICATION_SYSTEM_FINAL_DLX = "exchange.notification.system.final.dlx";
    public static final String ROUTING_NOTIFICATION_SYSTEM_FINAL = "notification.system.final";

    /**
     * 设备激活重试链路
     */
    public static final String QUEUE_DEVICE_ACTIVATION_RETRY = "queue.device.activation.retry";
    public static final String EXCHANGE_DEVICE_ACTIVATION_RETRY_DLX = "exchange.device.activation.retry.dlx";
    public static final String ROUTING_DEVICE_ACTIVATION_RETRY = "device.activation.retry";
    public static final String EXCHANGE_DEVICE_ACTIVATION_FINAL_DLX = "exchange.device.activation.final.dlx";
    public static final String ROUTING_DEVICE_ACTIVATION_FINAL = "device.activation.final";
    public static final String QUEUE_DEVICE_ACTIVATION_DLQ = "queue.device.activation.dlq";
}
