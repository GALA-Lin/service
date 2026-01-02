package com.unlimited.sports.globox.order.constants;

/**
 * redis 常量
 */
public class RedisConsts {
    /**
     * 分布式锁 key
     * order:lock:{orderNo}
     */
    public static final String ORDER_LOCK_KEY_PREFIX = "lock:order:";
}
