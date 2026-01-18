package com.unlimited.sports.globox.common.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;


@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RedisLock {

    /**
     * 锁 key（支持 SpEL）
     * 例："'lock:order:' + #orderNo"
     */
    String value();

    /**
     * 等待获取锁时间
     * 默认 200
     */
    long waitTime() default 500;

    /**
     * 锁自动释放时间（<=0 表示不设置 leaseTime，使用 watchdog 自动续期）
     */
    long leaseTime() default -1;

    /**
     * 等待时间单位
     */
    TimeUnit waitTimeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 是否公平锁
     */
    boolean fair() default false;

    /**
     * 获取锁失败时是否直接抛异常（true）或返回 null（false，通常不建议）
     */
    boolean failFast() default true;

    /**
     * key 前缀（可选，统一规范）
     */
    String prefix() default "";
}