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
     * 解析游标字符串
     * 格式：{LocalDateTime}|{noteId}
     * 示例：2025-12-28T10:00:00|123
     *
     * @param cursor 游标字符串
     * @return 游标对象，包含创建时间和笔记ID。如果cursor为null或空，返回null
     * @throws GloboxApplicationException 如果游标格式错误
     */
    public static Cursor parseCursor(String cursor) {
        if (cursor == null || cursor.trim().isEmpty()) {
            return null;
        }

        String[] parts = cursor.split("\\" + CURSOR_SEPARATOR, 2);
        if (parts.length != 2) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }

        try {
            LocalDateTime createdAt = LocalDateTime.parse(parts[0], ISO_FORMATTER);
            Long noteId = Long.parseLong(parts[1]);
            return new Cursor(createdAt, noteId);
        } catch (Exception e) {
            throw new GloboxApplicationException(SocialCode.NOTE_CURSOR_INVALID);
        }
    }

    /**
     * 构建游标字符串
     * 格式：{LocalDateTime}|{noteId}
     *
     * @param createdAt 创建时间
     * @param noteId    笔记ID
     * @return 游标字符串
     */
    public static String buildCursor(LocalDateTime createdAt, Long noteId) {
        if (createdAt == null || noteId == null) {
            return null;
        }
        return createdAt.format(ISO_FORMATTER) + CURSOR_SEPARATOR + noteId;
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
}

