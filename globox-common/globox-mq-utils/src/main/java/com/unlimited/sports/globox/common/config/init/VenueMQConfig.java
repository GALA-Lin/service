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

    @Value("${mq.consumer.retry.activity-reminder.retry-interval:1000}")
    private int activityReminderRetryInterval;

    @Value("${mq.consumer.retry.venue-booking-reminder.retry-interval:1000}")
    private int venueBookingReminderRetryInterval;

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
    public TopicExchange activityReminderRetryDlxExchange() {
        return new TopicExchange(VenueMQConstants.EXCHANGE_ACTIVITY_BOOKING_REMINDER_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange activityReminderFinalDlxExchange() {
        return new TopicExchange(VenueMQConstants.EXCHANGE_ACTIVITY_BOOKING_REMINDER_FINAL_DLX, true, false);
    }

    @Bean
    public Queue activityReminderQueue() {
        return QueueBuilder
                .durable(VenueMQConstants.QUEUE_ACTIVITY_BOOKING_REMINDER)
                .withArgument("x-dead-letter-exchange", VenueMQConstants.EXCHANGE_ACTIVITY_BOOKING_REMINDER_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", VenueMQConstants.ROUTING_ACTIVITY_BOOKING_REMINDER_RETRY)
                .build();
    }

    @Bean
    public Queue activityReminderRetryQueue() {
        return QueueBuilder.durable(VenueMQConstants.QUEUE_ACTIVITY_BOOKING_REMINDER_RETRY)
                .withArgument("x-message-ttl", activityReminderRetryInterval)
                .withArgument("x-dead-letter-exchange", VenueMQConstants.EXCHANGE_TOPIC_ACTIVITY_BOOKING_REMINDER)
                .withArgument("x-dead-letter-routing-key", VenueMQConstants.ROUTING_ACTIVITY_BOOKING_REMINDER)
                .build();
    }

    @Bean
    public Queue activityReminderDlq() {
        return QueueBuilder.durable(VenueMQConstants.QUEUE_ACTIVITY_BOOKING_REMINDER_DLQ).build();
    }

    // 主交换机 -> 主队列
    @Bean
    public Binding bindActivityReminderQueue() {
        return BindingBuilder.bind(activityReminderQueue())
                .to(activityReminderDelayExchange())
                .with(VenueMQConstants.ROUTING_ACTIVITY_BOOKING_REMINDER)
                .noargs();
    }

    // Retry-DLX -> retry.queue
    @Bean
    public Binding bindActivityReminderRetryQueue() {
        return BindingBuilder.bind(activityReminderRetryQueue())
                .to(activityReminderRetryDlxExchange())
                .with(VenueMQConstants.ROUTING_ACTIVITY_BOOKING_REMINDER_RETRY);
    }

    // Final-DLX -> 最终 DLQ
    @Bean
    public Binding bindActivityReminderDlq() {
        return BindingBuilder.bind(activityReminderDlq())
                .to(activityReminderFinalDlxExchange())
                .with(VenueMQConstants.ROUTING_ACTIVITY_BOOKING_REMINDER_FINAL);
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
    public TopicExchange venueBookingReminderRetryDlxExchange() {
        return new TopicExchange(VenueMQConstants.EXCHANGE_VENUE_BOOKING_REMINDER_RETRY_DLX, true, false);
    }

    @Bean
    public TopicExchange venueBookingReminderFinalDlxExchange() {
        return new TopicExchange(VenueMQConstants.EXCHANGE_VENUE_BOOKING_REMINDER_FINAL_DLX, true, false);
    }

    @Bean
    public Queue venueBookingReminderQueue() {
        return QueueBuilder
                .durable(VenueMQConstants.QUEUE_VENUE_BOOKING_REMINDER)
                .withArgument("x-dead-letter-exchange", VenueMQConstants.EXCHANGE_VENUE_BOOKING_REMINDER_RETRY_DLX)
                .withArgument("x-dead-letter-routing-key", VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER_RETRY)
                .build();
    }

    @Bean
    public Queue venueBookingReminderRetryQueue() {
        return QueueBuilder.durable(VenueMQConstants.QUEUE_VENUE_BOOKING_REMINDER_RETRY)
                .withArgument("x-message-ttl", venueBookingReminderRetryInterval)
                .withArgument("x-dead-letter-exchange", VenueMQConstants.EXCHANGE_TOPIC_VENUE_BOOKING_REMINDER)
                .withArgument("x-dead-letter-routing-key", VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER)
                .build();
    }

    @Bean
    public Queue venueBookingReminderDlq() {
        return QueueBuilder.durable(VenueMQConstants.QUEUE_VENUE_BOOKING_REMINDER_DLQ).build();
    }

    // 主交换机 -> 主队列
    @Bean
    public Binding bindVenueBookingReminderQueue() {
        return BindingBuilder.bind(venueBookingReminderQueue())
                .to(venueBookingReminderDelayExchange())
                .with(VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER)
                .noargs();
    }

    // Retry-DLX -> retry.queue
    @Bean
    public Binding bindVenueBookingReminderRetryQueue() {
        return BindingBuilder.bind(venueBookingReminderRetryQueue())
                .to(venueBookingReminderRetryDlxExchange())
                .with(VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER_RETRY);
    }

    // Final-DLX -> 最终 DLQ
    @Bean
    public Binding bindVenueBookingReminderDlq() {
        return BindingBuilder.bind(venueBookingReminderDlq())
                .to(venueBookingReminderFinalDlxExchange())
                .with(VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER_FINAL);
    }
}
