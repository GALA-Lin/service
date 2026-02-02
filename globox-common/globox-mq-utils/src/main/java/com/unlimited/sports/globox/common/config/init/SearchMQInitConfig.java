package com.unlimited.sports.globox.common.config.init;

import com.unlimited.sports.globox.common.constants.SearchMQConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 搜索服务MQ配置
 */
@Configuration
public class SearchMQInitConfig {

    /**
     * 搜索服务统一Topic Exchange
     */
    @Bean
    public TopicExchange searchExchange() {
        return new TopicExchange(
                SearchMQConstants.EXCHANGE_TOPIC_SEARCH,
                true,
                false);
    }

    /**
     * 笔记同步队列
     */
    @Bean
    public Queue noteSyncQueue() {
        return QueueBuilder
                .durable(SearchMQConstants.QUEUE_NOTE_SYNC)
                .build();
    }

    @Bean
    public Binding noteSyncBinding(Queue noteSyncQueue, TopicExchange searchExchange) {
        return BindingBuilder
                .bind(noteSyncQueue)
                .to(searchExchange)
                .with(SearchMQConstants.ROUTING_NOTE_SYNC);
    }
}
