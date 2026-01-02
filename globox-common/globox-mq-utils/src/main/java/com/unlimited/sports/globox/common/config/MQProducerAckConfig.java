package com.unlimited.sports.globox.common.config;

import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.model.MQRetryCorrelationData;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 用于绑定 生产者->交换机 与 交换机 -> 队列 失败后的回调方法到 rabbitTemplate
 */
@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    /**
     * 最大重发次数
     */
    private static final int MAX_RETRY = 5;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private JsonUtils jsonUtils;

    /**
     * bean实例化完成后执行
     * 绑定RabbitTemplate与下面两个回调方法
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    /**
     * 从生产者发送到交换机时触发
     *
     * @param correlationData 数据，必须在发送消息时传递一个 correlationData 对象，并且消息发送失败，才会有值
     * @param ack             是否发送成功
     * @param cause           发送失败原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (!ack) {
            log.warn("msgId：{} 发送失败 cause:{}， 数据：{}", correlationData.getId(), cause, jsonUtils.objectToJson(correlationData));
            this.retryMessage(correlationData);
        }
    }


    /**
     * 从交换机发送消息到队列 失败时触发（发送成功不触发）
     *
     * @param returned 返回的消息及其元数据
     */
    @Override
    public void returnedMessage(ReturnedMessage returned) {
        String exchange = returned.getExchange();

        Message message = returned.getMessage();
        String id = (String) message.getMessageProperties()
                .getHeaders()
                .get("spring_returned_message_correlation");

        if(exchange.equals(OrderMQConstants.EXCHANGE_TOPIC_ORDER_AUTO_CANCEL)){
            return;
        }

        log.warn("msgId:{} 未送达队列 消息主体:{} 应答码:{} 描述:{} exchange:{} routing:{}",
                id,
                new String(message.getBody()),
                returned.getReplyCode(),
                returned.getReplyText(),
                exchange,
                returned.getRoutingKey());

        String strJson = redisTemplate.opsForValue().get(id);
        if (ObjectUtils.isEmpty(strJson)) {
            log.error("msgId:{} 在 Redis 中不存在，无法重试", id);
            // TODO 写 DB / 告警
            return;
        }

        MQRetryCorrelationData correlationData =
                jsonUtils.jsonToPojo(strJson, MQRetryCorrelationData.class);

        retryMessage(correlationData);
    }


    /**
     * 重试发送消息
     *
     * @param correlationData 自己封装的实体类，记录了交换机、路由key等，用于消息重试发送
     */
    private void retryMessage(CorrelationData correlationData) {
        MQRetryCorrelationData MQRetryCorrelationData = (MQRetryCorrelationData) correlationData;

        // 判断是否到达重试次数
        int retryCount = MQRetryCorrelationData.getRetryCount();
        if (retryCount >= MAX_RETRY) {
            log.error("消息已重试达到最大次数，msgId:{}", correlationData.getId());
            // TODO ETA 2026/01/03 入库，短信、邮箱通知（待定）
            return;
        }

        // 重试次数 + 1
        MQRetryCorrelationData.increaseRetryCount();

        // 更新redis中的缓存
        redisTemplate.opsForValue().set(
                MQRetryCorrelationData.getId(),
                jsonUtils.objectToJson(MQRetryCorrelationData),
                10,
                TimeUnit.MINUTES
        );

        // 重试发送消息
        if (MQRetryCorrelationData.isDelay()) {
            // 如果是延迟队列
            rabbitTemplate.convertAndSend(MQRetryCorrelationData.getExchange(), MQRetryCorrelationData.getRoutingKey(), MQRetryCorrelationData.getMessage(), message -> {
                message.getMessageProperties().setDelay(MQRetryCorrelationData.getDelayTime() * 1000);
                return message;
            }, MQRetryCorrelationData);
        } else {
            // 如果不是延迟队列
            rabbitTemplate.convertAndSend(MQRetryCorrelationData.getExchange(), MQRetryCorrelationData.getRoutingKey(), MQRetryCorrelationData.getMessage(), MQRetryCorrelationData);
        }

        log.warn("msgId:{} 正在第【{}】次重试", correlationData.getId(), MQRetryCorrelationData.getRetryCount());
    }
}
