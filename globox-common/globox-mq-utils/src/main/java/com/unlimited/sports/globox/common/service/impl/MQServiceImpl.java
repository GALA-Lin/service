package com.unlimited.sports.globox.common.service.impl;

import com.unlimited.sports.globox.common.model.MQRetryCorrelationData;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
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
        MQRetryCorrelationData mqRetryCorrelationData = this.buildMQRetryCorrelationData(exchange, routingKey, message, false, null);

        // 存储到redis
        redisTemplate.opsForValue().set(
                Objects.requireNonNull(mqRetryCorrelationData.getId()),
                jsonUtils.objectToJson(mqRetryCorrelationData),
                10,
                TimeUnit.MINUTES
        );

        // 发送消息时将 CorrelationData 对象传入
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            String id = Objects.requireNonNull(mqRetryCorrelationData.getId());
            msg.getMessageProperties().setMessageId(id);
            msg.getMessageProperties().setHeader("x-msg-id", id);
            return msg;
        }, mqRetryCorrelationData);
        return true;
    }


    /**
     * 发送消息时，附加自定义的 header
     *
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息体
     * @param headers 自定义的消息头
     * @return 是否成功发送消息
     */
    @Override
    public boolean send(String exchange, String routingKey, Object message, Map<String, Object> headers) {
        MQRetryCorrelationData mqRetryCorrelationData = this.buildMQRetryCorrelationData(exchange, routingKey, message, false, null);

        // 存储到 redis（你原有逻辑）
        redisTemplate.opsForValue().set(
                Objects.requireNonNull(mqRetryCorrelationData.getId()),
                jsonUtils.objectToJson(mqRetryCorrelationData),
                10,
                TimeUnit.MINUTES
        );

        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            var mp = msg.getMessageProperties();

            // 1) messageId：只在原本为空时设置，避免覆盖已有 messageId
            if (!StringUtils.hasText(mp.getMessageId())) {
                mp.setMessageId(mqRetryCorrelationData.getId());
            }

            // 2) 自定义追踪 header：同样只在不存在时设置
            putIfAbsent(mp.getHeaders(), "x-msg-id", mqRetryCorrelationData.getId());

            // 3) 追加业务 headers：只追加，不覆盖默认/已有 header
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, Object> e : headers.entrySet()) {
                    String key = e.getKey();
                    Object value = e.getValue();
                    if (!StringUtils.hasText(key) || value == null) {
                        continue;
                    }
                    putIfAbsent(mp.getHeaders(), key, value);
                }
            }

            return msg;
        }, mqRetryCorrelationData);

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
        MQRetryCorrelationData mqRetryCorrelationData = this.buildMQRetryCorrelationData(exchange, routingKey, message, true, delay);

        // 存储到redis
        redisTemplate.opsForValue().set(
                Objects.requireNonNull(mqRetryCorrelationData.getId()),
                jsonUtils.objectToJson(mqRetryCorrelationData),
                10,
                TimeUnit.MINUTES);

        // 发送消息时将 延迟时间 与 CorrelationData 对象传入
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            String id = Objects.requireNonNull(mqRetryCorrelationData.getId());
            msg.getMessageProperties().setDelay(delay * 1000);
            msg.getMessageProperties().setMessageId(id);
            msg.getMessageProperties().setHeader("x-msg-id", id);
            return msg;
        }, mqRetryCorrelationData);
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


    /**
     * 只在 key 不存在时写入（不覆盖）
     */
    private void putIfAbsent(Map<String, Object> target, String key, Object value) {
        if (target.containsKey(key)) {
            if (log.isDebugEnabled()) {
                log.debug("[MQ] header exists, skip override. key={}, existing={}, new={}",
                        key, target.get(key), value);
            }
            return;
        }
        target.put(key, value);
    }
}
