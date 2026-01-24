package com.unlimited.sports.globox.common.config.init;

import com.unlimited.sports.globox.common.constants.CoachMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 教练相关 MQ 队列配置
 */
@Configuration
public class CoachMQInitConfig {

    /**
     * 教练课程提醒延迟消息（Coach Class Reminder）
     * 说明：
     * - 延迟到课程开始前1小时投递
     * - 主队列消费失败：reject(false) -> 进入 Retry-DLX -> Retry Queue(TTL) -> TTL 到期后回主 Exchange -> 主队列
     * - 超过最大次数：由业务主动投递到 Final-DLX -> DLQ
     */
    @Bean
    public CustomExchange coachClassReminderExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");

        return new CustomExchange(
                CoachMQConstants.EXCHANGE_TOPIC_COACH_CLASS_REMINDER,
                "x-delayed-message",
                true,
                false,
                args);
    }


    @Bean
    public Queue coachClassReminderQueue() {
        return QueueBuilder
                .durable(CoachMQConstants.QUEUE_COACH_CLASS_REMINDER_COACH)
                .build();
    }

    @Bean
    public Binding bindCoachClassReminderQueue() {
        return BindingBuilder
                .bind(coachClassReminderQueue())
                .to(coachClassReminderExchange())
                .with(CoachMQConstants.ROUTING_COACH_CLASS_REMINDER)
                .noargs();
    }

}
