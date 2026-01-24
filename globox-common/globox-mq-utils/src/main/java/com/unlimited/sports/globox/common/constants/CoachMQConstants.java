package com.unlimited.sports.globox.common.constants;

/**
 * 教练相关 MQ 常量
 */
public class CoachMQConstants {

    /**
     * 教练课程提醒延迟消息
     * 在订单支付成功后发送，延迟到课程开始前1小时投递
     * 消费时检查订单有效性，然后发送真正的通知
     */
    // 主交换机 & 主路由 & 主队列
    public static final String EXCHANGE_TOPIC_COACH_CLASS_REMINDER =
            "exchange.topic.coach.class-reminder";
    public static final String ROUTING_COACH_CLASS_REMINDER =
            "routing.coach.class-reminder";
    public static final String QUEUE_COACH_CLASS_REMINDER_COACH =
            "queue.coach.class-reminder.coach";

}
