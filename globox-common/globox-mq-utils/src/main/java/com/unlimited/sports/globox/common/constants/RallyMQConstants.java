package com.unlimited.sports.globox.common.constants;

/**
 * 约球服务 MQ 常量（约球即将开始提醒）
 */
public class RallyMQConstants {

    /**
     * 约球即将开始提醒
     * 主交换机 & 主路由 & 主队列
     */
    public static final String EXCHANGE_TOPIC_RALLY_STARTING_REMINDER =
            "exchange.topic.rally.starting-reminder";
    public static final String ROUTING_RALLY_STARTING_REMINDER =
            "routing.rally.starting-reminder";
    public static final String QUEUE_RALLY_STARTING_REMINDER =
            "queue.rally.starting-reminder";
}
