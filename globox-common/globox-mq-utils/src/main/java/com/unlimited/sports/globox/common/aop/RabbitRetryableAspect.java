package com.unlimited.sports.globox.common.aop;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.SpelMethodArgsUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.amqp.core.Message;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        Method specific = AopUtils.getMostSpecificMethod(method, pjp.getTarget().getClass());

        RabbitRetryable cfg = specific.getAnnotation(RabbitRetryable.class);

        // 运行期必填校验
        validateAnnotation(cfg, specific);

        Object[] allArgs = pjp.getArgs();

        // 约定：方法参数里必须包含 Channel 和 Message（amqpMessage）
        MethodArgs extracted = MethodArgs.from(allArgs);
        Channel channel = extracted.channel();
        Message amqpMessage = extracted.amqpMessage();
        Object payload = extracted.payload();

        long tag = amqpMessage.getMessageProperties().getDeliveryTag();

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
                // 超限：投 Final-DLX（带业务标识 headers） + ACK 原消息
                Map<String, Object> headers = buildFinalHeaders(cfg, specific, allArgs, payload, amqpMessage, ex);

                mqService.send(cfg.finalExchange(), cfg.finalRoutingKey(), payload, headers);

                channel.basicAck(tag, false);

                log.error("[MQ] 超过最大重试次数，已投递最终DLQ attempt={}, max={}, bizType={}, bizKey={}",
                        attempt, cfg.maxRetryCount(), cfg.bizType(),
                        headers.get("x-biz-key"));

            } else {
                // 未超限：Reject(false) 进入 Retry-DLX -> retry.queue(TTL) -> 回主队列
                channel.basicReject(tag, false);
                log.warn("[MQ] reject 进入重试链路 attempt={}, max={}", attempt, cfg.maxRetryCount());
            }

            return null;
        }
    }

    private void validateAnnotation(RabbitRetryable cfg, Method method) {
        if (cfg == null) {
            throw new IllegalArgumentException("@RabbitRetryable not found: " + method);
        }
        if (cfg.bizType() == null) {
            throw new IllegalArgumentException("@RabbitRetryable.bizType 必填: " + method);
        }
        if (!StringUtils.hasText(cfg.finalExchange())) {
            throw new IllegalArgumentException("@RabbitRetryable.finalExchange 必填: " + method);
        }
        if (!StringUtils.hasText(cfg.finalRoutingKey())) {
            throw new IllegalArgumentException("@RabbitRetryable.finalRoutingKey 必填: " + method);
        }
    }

    private Map<String, Object> buildFinalHeaders(RabbitRetryable cfg,
            Method specificMethod,
            Object[] allArgs,
            Object payload,
            Message amqpMessage,
            Throwable ex) {

        Map<String, Object> headers = new HashMap<>();

        // 业务类型：建议存 code
        MQBizTypeEnum bizType = cfg.bizType();
        headers.put("x-biz-type", bizType.getCode());

        // 业务 key：SpEL -> generator -> fallback
        String bizKey = resolveBizKey(cfg, specificMethod, allArgs, payload, amqpMessage);
        headers.put("x-biz-key", bizKey);

        // 原始路由
        headers.put("x-orig-exchange", amqpMessage.getMessageProperties().getReceivedExchange());
        headers.put("x-orig-routing-key", amqpMessage.getMessageProperties().getReceivedRoutingKey());

        // 异常摘要
        headers.put("x-exception", ex.getClass().getName());
        headers.put("x-exception-msg", safeMsg(ex.getMessage(), 256));

        return headers;
    }

    private String resolveBizKey(RabbitRetryable cfg,
            Method method,
            Object[] args,
            Object payload,
            Message amqpMessage) {

        // 1) bizKey：按 SpEL 解析（解析失败则走 generator）
        if (StringUtils.hasText(cfg.bizKey())) {
            String expr = cfg.bizKey().trim();
            try {
                String v = SpelMethodArgsUtils.eval(expr, method, args);
                if (StringUtils.hasText(v)) return v.trim();
            } catch (Exception e) {
                log.warn("[MQ] SpEL 解析失败，回退到 BizKeyGenerator. expr={}", expr, e);
            }
        }

        // 2) generator
        try {
            Class<? extends BizKeyGenerator> genClz = cfg.bizKeyGenerator();
            BizKeyGenerator generator = instantiate(genClz);
            String key = generator.generate(payload, amqpMessage);
            if (StringUtils.hasText(key)) return key.trim();
        } catch (Exception e) {
            log.warn("[MQ] BizKeyGenerator 执行失败，将回退到 fallback, generator={}",
                    cfg.bizKeyGenerator().getName(), e);
        }

        // 3) fallback
        String msgId = amqpMessage.getMessageProperties().getMessageId();
        if (StringUtils.hasText(msgId)) return "msgId:" + msgId;

        return "payloadHash:" + Objects.hashCode(payload);
    }

    private BizKeyGenerator instantiate(Class<? extends BizKeyGenerator> clazz) throws Exception {
        Constructor<? extends BizKeyGenerator> c = clazz.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }

    private String safeMsg(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
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
                else payload = arg;
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
        if (!(xDeath instanceof List<?> list) || list.isEmpty()) {
            return 0L;
        }
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> deathEntry)) {
            return 0L;
        }
        Object count = ((Map<String, Object>) deathEntry).get("count");
        if (count instanceof Long l) return l;
        if (count instanceof Integer i) return i.longValue();
        return 0L;
    }
}