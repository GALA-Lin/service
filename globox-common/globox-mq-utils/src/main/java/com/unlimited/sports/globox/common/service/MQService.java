package com.unlimited.sports.globox.common.service;


import java.util.Map;

/**
 * 消息发送服务
 */
public interface MQService {

    /**
     * 发送消息
     */
    boolean send(String exchange, String routingKey, Object message);

    /**
     * 发送消息是，附加自定义的 header
     */
    boolean send(String exchange, String routingKey, Object message, Map<String, Object> headers);

    /**
     * 发送延迟消息
     */
    boolean sendDelay(String exchange, String routingKey, Object message, Integer delay);

}
