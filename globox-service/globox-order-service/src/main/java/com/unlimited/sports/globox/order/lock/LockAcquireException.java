package com.unlimited.sports.globox.order.lock;


import com.unlimited.sports.globox.common.exception.GloboxApplicationException;

/**
 * 此异常通常用于无法获得锁的情况，
 * 例如当使用{@link RedisLock}注释时，操作未能在指定的参数内获得必要的锁。
 */
public class LockAcquireException extends GloboxApplicationException {
    public LockAcquireException(String message) {
        super(message);
    }
}