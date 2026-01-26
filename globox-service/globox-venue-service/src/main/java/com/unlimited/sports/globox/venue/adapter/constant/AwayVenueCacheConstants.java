package com.unlimited.sports.globox.venue.adapter.constant;

import java.time.LocalDate;

/**
 * Away球场适配器缓存常量统一管理
 * 包含所有第三方平台（Aitennis、Changxiaoer等）的缓存常量
 */
public class AwayVenueCacheConstants {

    /**
     * 槽位缓存 - 前缀
     * 格式: third_party:slots:{venueId}:{date}
     */
    public static final String SLOTS_CACHE_KEY_PREFIX = "third_party:slots:";

    /**
     * 槽位缓存 - TTL（分钟）
     * 5分钟过期，与lock/unlock操作的缓存清除策略同步
     */
    public static final long SLOTS_CACHE_TTL_MINUTES = 5;

    /**
     * 构建槽位缓存key
     *
     * @param venueId 场馆ID
     * @param date    日期（ yyyy-MM-dd 格式）
     * @return 缓存key
     */
    public static String buildSlotsCacheKey(Long venueId, String date) {
        return SLOTS_CACHE_KEY_PREFIX + venueId + ":" + date;
    }

    /**
     * 构建槽位缓存key
     *
     * @param venueId 场馆ID
     * @param date    日期（LocalDate对象）
     * @return 缓存key
     */
    public static String buildSlotsCacheKey(Long venueId, LocalDate date) {
        return buildSlotsCacheKey(venueId, date.toString());
    }
}
