package com.unlimited.sports.globox.social.consts;

/**
 * 社交模块 redis 常量
 */
public class SocialRedisKeyConstants {
    public static final String REDIS_IM_USER_SIG = "silence:test_im_user_sig:";

    // 点赞事件 Set - 待同步
    public static final String LIKE_EVENTS_PENDING = "note:like:pending";

    // 点赞事件 Set - 同步中
    public static final String LIKE_EVENTS_PROCESSING = "note:like:processing";

    // 单次同步数量
    public static final Integer LIKE_SYNC_BATCH_SIZE = 500;
}
