package com.unlimited.sports.globox.venue.constants;

/**
 * 订场缓存常量
 */
public class BookingCacheConstants {

    /**
     * 防止并发超买分布式锁前缀
     */
    public static final String BOOKING_LOCK_KEY_PREFIX = "venue:book:slot:lock:";


    public static final String BOOKING_LOCK_KEY_SEPARATOR = ":";
}
