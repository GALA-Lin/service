package com.unlimited.sports.globox.social.util;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.SocialCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 游标工具类
 * 用于解析和构建游标字符串
 *
 * @author Wreckloud
 * @since 2025/12/28
 */
public class CursorUtils {

    /**
     * 游标分隔符
     */
    private static final String CURSOR_SEPARATOR = "|";

    /**
     * ISO-8601 日期时间格式
     */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 解析最新流游标（格式：latest|{createdAt}|{noteId}）
     * 示例：latest|2025-12-28T10:00:00|123
     *
     * @param cursor 游标字符串
     * @return 游标对象，包含创建时间和笔记ID。如果cursor为null或空，返回null
     * @throws GloboxApplicationException 如果游标格式错误或前缀不匹配
     */
    public static Cursor parseLatestCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        String[] parts = cursor.split("\\" + CURSOR_SEPARATOR, 3);
        if (parts.length != 3) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        // 校验前缀
        if (!"latest".equals(parts[0])) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        try {
            LocalDateTime createdAt = LocalDateTime.parse(parts[1], ISO_FORMATTER);
            Long noteId = Long.parseLong(parts[2]);
            return new Cursor(createdAt, noteId);
        } catch (Exception e) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }
    }

    /**
     * 构建最新流游标字符串（格式：latest|{createdAt}|{noteId}）
     *
     * @param createdAt 创建时间
     * @param noteId    笔记ID
     * @return 游标字符串
     */
    public static String buildLatestCursor(LocalDateTime createdAt, Long noteId) {
        if (createdAt == null || noteId == null) {
            return null;
        }
        return "latest" + CURSOR_SEPARATOR + createdAt.format(ISO_FORMATTER) + CURSOR_SEPARATOR + noteId;
    }

    /**
     * 解析推荐流游标（格式：pool|{seed}|{createdAt}|{noteId}）
     * 示例：pool|123456|2025-12-28T10:00:00|123
     *
     * @param cursor 游标字符串
     * @return 推荐流游标对象，包含seed、创建时间和笔记ID
     * @throws GloboxApplicationException 如果游标格式错误或前缀不匹配
     */
    public static PoolCursor parsePoolCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        String[] parts = cursor.split("\\" + CURSOR_SEPARATOR, 4);
        if (parts.length != 4) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        // 校验前缀
        if (!"pool".equals(parts[0])) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        try {
            Long seed = Long.parseLong(parts[1]);
            LocalDateTime createdAt = LocalDateTime.parse(parts[2], ISO_FORMATTER);
            Long noteId = Long.parseLong(parts[3]);
            return new PoolCursor(seed, createdAt, noteId);
        } catch (Exception e) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }
    }

    /**
     * 构建推荐流游标字符串（格式：pool|{seed}|{createdAt}|{noteId}）
     *
     * @param seed      随机种子
     * @param createdAt 创建时间
     * @param noteId    笔记ID
     * @return 游标字符串
     */
    public static String buildPoolCursor(Long seed, LocalDateTime createdAt, Long noteId) {
        if (seed == null || createdAt == null || noteId == null) {
            return null;
        }
        return "pool" + CURSOR_SEPARATOR + seed + CURSOR_SEPARATOR + 
               createdAt.format(ISO_FORMATTER) + CURSOR_SEPARATOR + noteId;
    }

    /**
     * 解析最热流游标（格式：hot|{likeCount}|{createdAt}|{noteId}）
     * 示例：hot|100|2025-12-28T10:00:00|123
     *
     * @param cursor 游标字符串
     * @return 最热流游标对象
     * @throws GloboxApplicationException 如果游标格式错误或前缀不匹配
     */
    public static HotCursor parseHotCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        String[] parts = cursor.split("\\" + CURSOR_SEPARATOR, 4);
        if (parts.length != 4) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        // 校验前缀
        if (!"hot".equals(parts[0])) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        try {
            Integer likeCount = Integer.parseInt(parts[1]);
            LocalDateTime createdAt = LocalDateTime.parse(parts[2], ISO_FORMATTER);
            Long noteId = Long.parseLong(parts[3]);
            return new HotCursor(likeCount, createdAt, noteId);
        } catch (Exception e) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }
    }

    /**
     * 构建最热流游标字符串（格式：hot|{likeCount}|{createdAt}|{noteId}）
     *
     * @param likeCount 点赞数
     * @param createdAt 创建时间
     * @param noteId    笔记ID
     * @return 游标字符串
     */
    public static String buildHotCursor(Integer likeCount, LocalDateTime createdAt, Long noteId) {
        if (likeCount == null || createdAt == null || noteId == null) {
            return null;
        }
        return "hot" + CURSOR_SEPARATOR + likeCount + CURSOR_SEPARATOR + 
               createdAt.format(ISO_FORMATTER) + CURSOR_SEPARATOR + noteId;
    }

    /**
     * 解析点赞列表游标（格式：{likeCreatedAt}|{likeId}）
     * 示例：2025-12-28T10:00:00|123
     *
     * @param cursor 游标字符串
     * @return 点赞列表游标对象
     * @throws GloboxApplicationException 如果游标格式错误
     */
    public static LikedCursor parseLikedCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        String[] parts = cursor.split("\\" + CURSOR_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        try {
            LocalDateTime likeCreatedAt = LocalDateTime.parse(parts[0], ISO_FORMATTER);
            Long likeId = Long.parseLong(parts[1]);
            return new LikedCursor(likeCreatedAt, likeId);
        } catch (Exception e) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }
    }

    /**
     * 构建点赞列表游标字符串（格式：{likeCreatedAt}|{likeId}）
     *
     * @param likeCreatedAt 点赞时间
     * @param likeId        点赞ID
     * @return 游标字符串
     */
    public static String buildLikedCursor(LocalDateTime likeCreatedAt, Long likeId) {
        if (likeCreatedAt == null || likeId == null) {
            return null;
        }
        return likeCreatedAt.format(ISO_FORMATTER) + CURSOR_SEPARATOR + likeId;
    }

    /**
     * 解析评论列表游标（格式：{createdAt}|{commentId}）
     * 示例：2025-12-28T10:00:00|123
     *
     * @param cursor 游标字符串
     * @return 评论列表游标对象，如果cursor为null或空，返回null
     * @throws GloboxApplicationException 如果游标格式错误
     */
    public static CommentCursor parseCommentCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        String[] parts = cursor.split("\\" + CURSOR_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new GloboxApplicationException(SocialCode.COMMENT_CURSOR_INVALID);
        }

        try {
            LocalDateTime createdAt = LocalDateTime.parse(parts[0], ISO_FORMATTER);
            Long commentId = Long.parseLong(parts[1]);
            return new CommentCursor(createdAt, commentId);
        } catch (Exception e) {
            throw new GloboxApplicationException(SocialCode.COMMENT_CURSOR_INVALID);
        }
    }

    /**
     * 构建评论列表游标字符串
     * 格式：{createdAt}|{commentId}
     *
     * @param createdAt 创建时间
     * @param commentId 评论ID
     * @return 游标字符串
     */
    public static String buildCommentCursor(LocalDateTime createdAt, Long commentId) {
        if (createdAt == null || commentId == null) {
            return null;
        }
        return createdAt.format(ISO_FORMATTER) + CURSOR_SEPARATOR + commentId;
    }

    /**
     * 游标对象
     */
    public static class Cursor {
        private final LocalDateTime createdAt;
        private final Long noteId;

        public Cursor(LocalDateTime createdAt, Long noteId) {
            this.createdAt = createdAt;
            this.noteId = noteId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public Long getNoteId() {
            return noteId;
        }
    }

    /**
     * 推荐流游标对象
     */
    public static class PoolCursor {
        private final Long seed;
        private final LocalDateTime createdAt;
        private final Long noteId;

        public PoolCursor(Long seed, LocalDateTime createdAt, Long noteId) {
            this.seed = seed;
            this.createdAt = createdAt;
            this.noteId = noteId;
        }

        public Long getSeed() {
            return seed;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public Long getNoteId() {
            return noteId;
        }
    }

    /**
     * 最热流游标对象
     */
    public static class HotCursor {
        private final Integer likeCount;
        private final LocalDateTime createdAt;
        private final Long noteId;

        public HotCursor(Integer likeCount, LocalDateTime createdAt, Long noteId) {
            this.likeCount = likeCount;
            this.createdAt = createdAt;
            this.noteId = noteId;
        }

        public Integer getLikeCount() {
            return likeCount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public Long getNoteId() {
            return noteId;
        }
    }

    /**
     * 点赞列表游标对象
     */
    public static class LikedCursor {
        private final LocalDateTime likeCreatedAt;
        private final Long likeId;

        public LikedCursor(LocalDateTime likeCreatedAt, Long likeId) {
            this.likeCreatedAt = likeCreatedAt;
            this.likeId = likeId;
        }

        public LocalDateTime getLikeCreatedAt() {
            return likeCreatedAt;
        }

        public Long getLikeId() {
            return likeId;
        }
    }

    /**
     * 评论列表游标对象
     */
    public static class CommentCursor {
        private final LocalDateTime createdAt;
        private final Long commentId;

        public CommentCursor(LocalDateTime createdAt, Long commentId) {
            this.createdAt = createdAt;
            this.commentId = commentId;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public Long getCommentId() {
            return commentId;
        }
    }
}

