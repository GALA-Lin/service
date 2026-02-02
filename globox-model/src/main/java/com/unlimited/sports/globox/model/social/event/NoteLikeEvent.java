package com.unlimited.sports.globox.model.social.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 笔记点赞事件
 * 用于 Redis Stream 存储和异步处理
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoteLikeEvent {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 操作类型：LIKE（点赞）或 UNLIKE（取消点赞）
     */
    private LikeAction action;

    /**
     * 该记录在数据库中是否存在
     */
    private Boolean existsInDb;

    /**
     * 如果存在，是否被软删除
     */
    private Boolean isDeletedInDb;

    /**
     * 操作类型枚举
     */
    public enum LikeAction {
        LIKE,
        UNLIKE
    }

    private static final ObjectMapper objectMapper = new ObjectMapper();



    /**
     * 从 JSON 字符串解析事件
     */
    public static NoteLikeEvent fromSetValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, NoteLikeEvent.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取事件的唯一键（用于去重）
     * 格式: userId:noteId
     */
    public String toEventKey() {
        return String.format("%d:%d", userId, noteId);
    }
}
