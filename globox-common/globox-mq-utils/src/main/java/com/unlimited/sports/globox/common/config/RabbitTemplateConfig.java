package com.unlimited.sports.globox.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTemplateConfig implements InitializingBean {

    /**
     * 自动注入RabbitTemplate模板
     */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;



    /**
     * 发送消息JSON序列化
     */
    @Override
    public void afterPropertiesSet() {
        //使用JSON序列化
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
    }
}