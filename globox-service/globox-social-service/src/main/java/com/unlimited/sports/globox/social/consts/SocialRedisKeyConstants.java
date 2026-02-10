package com.unlimited.sports.globox.social.consts;

/**
 * 社交模块 redis 常量
 */
public class SocialRedisKeyConstants {
    public static final String REDIS_IM_USER_SIG = "silence:test_im_user_sig:";

    // 点赞事件 Hash - 待同步
    public static final String LIKE_EVENTS_PENDING = "note:like:pending";

    // 点赞事件 Hash - 同步中 (RENAME 快照)
    public static final String LIKE_EVENTS_PROCESSING = "note:like:processing";

    // 点赞增量 Hash: field=noteId, value=delta
    public static final String NOTE_LIKE_DELTA = "note:engagement:like:delta";
    public static final String NOTE_LIKE_DELTA_PROCESSING = "note:engagement:like:delta:processing";

    // 评论增量 Hash: field=noteId, value=delta
    public static final String NOTE_COMMENT_DELTA = "note:engagement:comment:delta";
    public static final String NOTE_COMMENT_DELTA_PROCESSING = "note:engagement:comment:delta:processing";

    // 帖子点赞通知去重（1天内同一点赞人对同一帖子只通知一次）
    public static final String NOTE_LIKE_NOTIFY_DEDUP_PREFIX = "social:notify:note_like:dedup:";
}
