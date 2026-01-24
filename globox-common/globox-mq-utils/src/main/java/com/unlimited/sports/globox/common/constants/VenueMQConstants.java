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
     *  订场即将开始提醒
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_VENUE_BOOKING_REMINDER =
            "exchange.topic.venue.booking-reminder";
    public static final String ROUTING_VENUE_BOOKING_REMINDER =
            "routing.venue.booking-reminder";
    public static final String QUEUE_VENUE_BOOKING_REMINDER =
            "queue.venue.booking-reminder";

}
