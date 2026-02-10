package com.unlimited.sports.globox.model.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 笔记互动数据增量同步MQ消息
 * 包含一批笔记的点赞/评论增量，用于增量更新ES
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteEngagementSyncMessage implements Serializable {

    /**
     * 互动数据列表
     */
    private List<NoteEngagementItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoteEngagementItem implements Serializable {

        /**
         * 笔记ID
         */
        private Long noteId;

        /**
         * 点赞增量（正数=新增点赞，负数=取消点赞）
         */
        private Integer likeDelta;

        /**
         * 评论增量（正数=新增评论，负数=删除评论）
         */
        private Integer commentDelta;
    }
}
