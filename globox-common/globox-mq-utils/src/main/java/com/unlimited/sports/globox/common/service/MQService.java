package com.unlimited.sports.globox.common.service;


/**
 * 消息发送服务
 */
public interface MQService {

    /**
     * 发送消息
     */
    boolean send(String exchange, String routingKey, Object message);

    /**
     * 发送延迟消息
     */
    boolean sendDelay(String exchange, String routingKey, Object message, Integer delay);

}
