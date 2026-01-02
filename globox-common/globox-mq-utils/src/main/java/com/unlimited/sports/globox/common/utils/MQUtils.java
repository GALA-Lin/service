package com.unlimited.sports.globox.common.utils;

import org.springframework.amqp.core.Message;

import java.util.List;
import java.util.Map;

/**
 * 消息队列工具类
 */
public class MQUtils {

    public static long getXDeathCount(Message amqpMessage) {
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
