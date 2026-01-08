package com.unlimited.sports.globox.common.constants;

/**
 * 场馆服务 MQ 常量（包含订场提醒和活动提醒）
 */
public class VenueMQConstants {

    /**
     *  活动即将开始提醒
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_ACTIVITY_BOOKING_REMINDER =
            "exchange.topic.activity.booking-reminder";
    public static final String ROUTING_ACTIVITY_BOOKING_REMINDER =
            "routing.activity.booking-reminder";
    public static final String QUEUE_ACTIVITY_BOOKING_REMINDER =
            "queue.activity.booking-reminder";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_ACTIVITY_BOOKING_REMINDER_RETRY =
            "queue.activity.booking-reminder.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_ACTIVITY_BOOKING_REMINDER_RETRY_DLX =
            "exchange.topic.activity.booking-reminder.retry.dlx";
    public static final String ROUTING_ACTIVITY_BOOKING_REMINDER_RETRY =
            "routing.activity.booking-reminder.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_ACTIVITY_BOOKING_REMINDER_FINAL_DLX =
            "exchange.topic.activity.booking-reminder.final.dlx";
    public static final String ROUTING_ACTIVITY_BOOKING_REMINDER_FINAL =
            "routing.activity.booking-reminder.final";
    public static final String QUEUE_ACTIVITY_BOOKING_REMINDER_DLQ =
            "queue.activity.booking-reminder.dlq";

    /**
     *  订场即将开始提醒
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_VENUE_BOOKING_REMINDER =
            "exchange.topic.venue.booking-reminder";
    public static final String ROUTING_VENUE_BOOKING_REMINDER =
            "routing.venue.booking-reminder";
    public static final String QUEUE_VENUE_BOOKING_REMINDER =
            "queue.venue.booking-reminder";

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_VENUE_BOOKING_REMINDER_RETRY =
            "queue.venue.booking-reminder.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_VENUE_BOOKING_REMINDER_RETRY_DLX =
            "exchange.topic.venue.booking-reminder.retry.dlx";
    public static final String ROUTING_VENUE_BOOKING_REMINDER_RETRY =
            "routing.venue.booking-reminder.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_VENUE_BOOKING_REMINDER_FINAL_DLX =
            "exchange.topic.venue.booking-reminder.final.dlx";
    public static final String ROUTING_VENUE_BOOKING_REMINDER_FINAL =
            "routing.venue.booking-reminder.final";
    public static final String QUEUE_VENUE_BOOKING_REMINDER_DLQ =
            "queue.venue.booking-reminder.dlq";
}
