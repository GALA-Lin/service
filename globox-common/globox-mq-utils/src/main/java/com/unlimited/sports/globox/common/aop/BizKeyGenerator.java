package com.unlimited.sports.globox.common.aop;

import org.springframework.amqp.core.Message;

/**
 * 业务 key 生成规则
 */
public interface BizKeyGenerator {
    /**
     * 生成 业务 key
     *
     * @param payload     业务消息体
     * @param amqpMessage 原始 AMQP Message（可取 headers 等）
     * @return bizKey（不能为空）
     */
    String generate(Object payload, Message amqpMessage);
}
