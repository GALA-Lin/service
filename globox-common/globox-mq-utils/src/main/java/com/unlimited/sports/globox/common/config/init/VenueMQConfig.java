package com.unlimited.sports.globox.common.config.init;

import com.unlimited.sports.globox.common.constants.VenueMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 场馆服务 MQ 配置
 * 包括：活动提醒延迟消息、订场提醒延迟消息
 * 使用延迟消息插件实现延迟功能
 */
@Configuration
public class VenueMQConfig {

    //  活动提醒相关配置

    /**
     * 活动提醒延迟消息交换机
     */
    @Bean
    public CustomExchange activityReminderDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");

        return new CustomExchange(
                VenueMQConstants.EXCHANGE_TOPIC_ACTIVITY_BOOKING_REMINDER,
                "x-delayed-message",
                true,
                false,
                args);
    }

    @Bean
    public Queue activityReminderQueue() {
        return QueueBuilder
                .durable(VenueMQConstants.QUEUE_ACTIVITY_BOOKING_REMINDER)
                .build();
    }

    // 主交换机 -> 主队列
    @Bean
    public Binding bindActivityReminderQueue() {
        return BindingBuilder.bind(activityReminderQueue())
                .to(activityReminderDelayExchange())
                .with(VenueMQConstants.ROUTING_ACTIVITY_BOOKING_REMINDER)
                .noargs();
    }

    //  订场提醒相关配置

    /**
     * 订场提醒延迟消息交换机
     */
    @Bean
    public CustomExchange venueBookingReminderDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "topic");

        return new CustomExchange(
                VenueMQConstants.EXCHANGE_TOPIC_VENUE_BOOKING_REMINDER,
                "x-delayed-message",
                true,
                false,
                args);
    }


    @Bean
    public Queue venueBookingReminderQueue() {
        return QueueBuilder
                .durable(VenueMQConstants.QUEUE_VENUE_BOOKING_REMINDER)
                .build();
    }

    // 主交换机 -> 主队列
    @Bean
    public Binding bindVenueBookingReminderQueue() {
        return BindingBuilder.bind(venueBookingReminderQueue())
                .to(venueBookingReminderDelayExchange())
                .with(VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER)
                .noargs();
    }
}
