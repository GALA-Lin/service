package com.unlimited.sports.globox.common.service.impl;

import com.unlimited.sports.globox.common.model.MQRetryCorrelationData;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 消息发送服务
 */
@Slf4j
@Service
public class MQServiceImpl implements MQService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JsonUtils jsonUtils;


    /**
     * 发送消息
     *
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息体
     * @return 是否成功发送
     */
    public boolean send(String exchange, String routingKey, Object message) {

        // 发送消息前，构建实体类
        MQRetryCorrelationData MQRetryCorrelationData = this.buildMQRetryCorrelationData(exchange, routingKey, message, false, null);

        // 存储到redis
        redisTemplate.opsForValue().set(
                Objects.requireNonNull(MQRetryCorrelationData.getId()),
                jsonUtils.objectToJson(MQRetryCorrelationData),
                10,
                TimeUnit.MINUTES
        );

        // 发送消息时将 CorrelationData 对象传入
        rabbitTemplate.convertAndSend(exchange, routingKey, message, MQRetryCorrelationData);
        return true;
    }


    /**
     * 发送延迟消息
     *
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param message 消息体
     * @param delay 延迟时间 （单位 s）
     * @return 是否成功发送
     */
    public boolean sendDelay(String exchange, String routingKey, Object message, Integer delay) {
        // 发送消息前，构建实体类
        MQRetryCorrelationData MQRetryCorrelationData = this.buildMQRetryCorrelationData(exchange, routingKey, message, true, delay);

        // 存储到redis
        redisTemplate.opsForValue().set(
                Objects.requireNonNull(MQRetryCorrelationData.getId()),
                jsonUtils.objectToJson(MQRetryCorrelationData),
                10,
                TimeUnit.MINUTES);

        // 发送消息时将 延迟时间 与 CorrelationData 对象传入
        rabbitTemplate.convertAndSend(exchange, routingKey, message, message1 -> {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            log.info("测试延时队列：发送消息时间：{}，消息：{}",sdf.format(new Date()),message);

            message1.getMessageProperties().setDelay(delay * 1000);
            return message1;
        }, MQRetryCorrelationData);
        return true;
    }


    /**
     * 封装 MQRetryCorrelationData 对象
     */
    private MQRetryCorrelationData buildMQRetryCorrelationData(String exchange, String routingKey, Object message, Boolean isDelay, Integer delay) {
        MQRetryCorrelationData MQRetryCorrelationData = new MQRetryCorrelationData();
        // 设置id
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        MQRetryCorrelationData.setId(id);
        // 设置消息主体
        MQRetryCorrelationData.setMessage(message);
        // 设置交换机
        MQRetryCorrelationData.setExchange(exchange);
        // 设置routingKey
        MQRetryCorrelationData.setRoutingKey(routingKey);
        // 是否是延迟消息
        if (isDelay) {
            MQRetryCorrelationData.setDelay(true);
            MQRetryCorrelationData.setDelayTime(delay);
        }
        return MQRetryCorrelationData;
    }



}
