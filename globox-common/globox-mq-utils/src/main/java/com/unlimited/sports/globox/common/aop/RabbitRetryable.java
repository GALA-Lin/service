package com.unlimited.sports.globox.common.aop;

import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;

import java.lang.annotation.*;

/**
 * 该注解用于标记RabbitMQ消息消费方法，使其具备重试机制。当标记的方法在处理消息时发生异常，会根据配置的参数决定是否重试以及如何处理超过最大重试次数的消息。
 * 注意：1. 业务方法建议不要 ack 了，统一由 AOP 做
 *      2. 强约束：listener 方法必须带 (payload, Channel, Message)（顺序不限）
 * @see RabbitRetryableAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitRetryable {

    /**
     * 最大重试次数（attempt），默认 5
     * attempt = xDeathCount + 1（把“本次失败也算一次”）
     */
    int maxRetryCount() default 5;

    /**
     * 超限后投递到 Final-DLX 的 exchange
     */
    String finalExchange();

    /**
     * 超限后投递到 Final-DLX 的 routingKey
     */
    String finalRoutingKey();

    /**
     * 业务类型
     */
    MQBizTypeEnum bizType();

    /**
     * 业务 key
     */
    String bizKey() default "";

    /**
     * 业务 key 生成器
     * 优先加载 com.unlimited.sports.globox.common.aop.RabbitRetryable#bizKey()
     */
    Class<? extends BizKeyGenerator> bizKeyGenerator() default DefaultBizKeyGenerator.class;
}