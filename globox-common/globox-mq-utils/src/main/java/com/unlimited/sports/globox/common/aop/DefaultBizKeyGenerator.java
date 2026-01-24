package com.unlimited.sports.globox.common.aop;

import org.springframework.amqp.core.Message;

import java.util.Objects;

/**
 * 默认的业务键生成器实现类。
 * 该类实现了 BizKeyGenerator 接口，用于根据给定的消息体和AMQP消息生成一个唯一的业务键。
 * 生成规则如下：
 * - 如果 AMQP 消息中存在非空且非空白的 messageId，则使用"msgId:原messageId"作为业务键。
 * - 否则，使用"bodyHash:payload哈希值"作为业务键。这里payload哈希值是通过调用Objects.hashCode(payload)计算得到的。
 */
public class DefaultBizKeyGenerator implements BizKeyGenerator {
    @Override
    public String generate(Object payload, Message amqpMessage) {
        String msgId = amqpMessage.getMessageProperties().getMessageId();
        if (msgId != null && !msgId.isBlank()) {
            return "msgId:" + msgId;
        }
        return "bodyHash:" + Objects.hashCode(payload);
    }
}