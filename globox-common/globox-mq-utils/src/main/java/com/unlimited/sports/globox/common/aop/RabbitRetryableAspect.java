package com.unlimited.sports.globox.common.aop;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.service.MQService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.core.Message;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

@Slf4j
@Aspect
@Component
@AllArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RabbitRetryableAspect {

    private final MQService mqService;

    @Around("@annotation(com.unlimited.sports.globox.common.aop.RabbitRetryable)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        Method specific = org.springframework.aop.support.AopUtils
                .getMostSpecificMethod(method, pjp.getTarget().getClass());

        RabbitRetryable cfg = specific.getAnnotation(RabbitRetryable.class);

        // 约定：方法参数里必须包含 Channel 和 Message（amqpMessage）
        MethodArgs args = MethodArgs.from(pjp.getArgs());
        Channel channel = args.channel();
        Message amqpMessage = args.amqpMessage();

        long tag = amqpMessage.getMessageProperties().getDeliveryTag();
        Object payload = args.payload(); // 你的 OrderAutoCancelMessage 等

        try {
            Object ret = pjp.proceed();

            channel.basicAck(tag, false);
            return ret;

        } catch (Throwable ex) {

            long deathCount = getXDeathCount(amqpMessage);
            long attempt = deathCount + 1;

            log.error("[MQ] 消费失败, xDeathCount={}, attempt={}, max={}",
                    deathCount, attempt, cfg.maxRetryCount(), ex);

            if (attempt > cfg.maxRetryCount()) {
                // 超限：投 Final-DLX + ACK 原消息（避免继续重试）
                mqService.send(cfg.finalExchange(), cfg.finalRoutingKey(), payload);
                channel.basicAck(tag, false);

                log.error("[MQ] 超过最大重试次数，已投递最终DLQ attempt={}, max={}",
                        attempt, cfg.maxRetryCount());
            } else {
                // 未超限：Reject(false) 进入 Retry-DLX -> retry.queue(TTL) -> 回主队列
                channel.basicReject(tag, false);

                log.warn("[MQ] reject 进入重试链路 attempt={}, max={}",
                        attempt, cfg.maxRetryCount());
            }

            // 注意：这里不要再把异常抛出去，否则容器可能二次处理/重复 nack
            return null;
        }
    }

    /**
     * 从参数中提取 payload / channel / amqpMessage
     * 强约束：listener 方法必须带 (payload, Channel, Message)（顺序不限）
     */
    private record MethodArgs(Object payload, Channel channel, Message amqpMessage) {
        static MethodArgs from(Object[] args) {
            Object payload = null;
            Channel channel = null;
            Message message = null;

            for (Object arg : args) {
                if (arg == null) continue;
                if (arg instanceof Channel ch) channel = ch;
                else if (arg instanceof Message msg) message = msg;
                else payload = arg; // 默认第一个非 Channel/Message 的就是 payload
            }
            if (payload == null || channel == null || message == null) {
                throw new IllegalArgumentException(
                        "@RabbitRetryable 方法参数必须包含 payload + Channel + org.springframework.amqp.core.Message");
            }
            return new MethodArgs(payload, channel, message);
        }
    }


    private long getXDeathCount(Message amqpMessage) {
        Object xDeath = amqpMessage.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeath instanceof List<?> list)) {
            return 0L;
        }
        if (list.isEmpty()) {
            return 0L;
        }
        Object first = list.get(0);
        if (!(first instanceof Map)) {
            return 0L;
        }
        Map<String, Object> deathEntry = (Map<String, Object>) first;
        Object count = deathEntry.get("count");
        if (count instanceof Long) return (Long) count;
        if (count instanceof Integer) return ((Integer) count).longValue();
        return 0L;
    }
}