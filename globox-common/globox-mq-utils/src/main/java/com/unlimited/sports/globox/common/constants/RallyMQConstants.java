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

    /**
     * 重试队列（TTL）
     */
    public static final String QUEUE_RALLY_STARTING_REMINDER_RETRY =
            "queue.rally.starting-reminder.retry";

    /**
     * Retry-DLX：主队列失败后进入重试队列
     */
    public static final String EXCHANGE_RALLY_STARTING_REMINDER_RETRY_DLX =
            "exchange.topic.rally.starting-reminder.retry.dlx";
    public static final String ROUTING_RALLY_STARTING_REMINDER_RETRY =
            "routing.rally.starting-reminder.retry";

    /**
     * Final-DLX：超过最大次数进入最终 DLQ
     */
    public static final String EXCHANGE_RALLY_STARTING_REMINDER_FINAL_DLX =
            "exchange.topic.rally.starting-reminder.final.dlx";
    public static final String ROUTING_RALLY_STARTING_REMINDER_FINAL =
            "routing.rally.starting-reminder.final";
    public static final String QUEUE_RALLY_STARTING_REMINDER_DLQ =
            "queue.rally.starting-reminder.dlq";
}
