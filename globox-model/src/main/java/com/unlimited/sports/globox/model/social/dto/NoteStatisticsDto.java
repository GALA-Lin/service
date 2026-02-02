package com.unlimited.sports.globox.model.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 笔记统计信息DTO
 * 包含笔记的点赞数、评论数、用户是否点赞等实时数据
 * 综合数据库和Redis中未同步的事件信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteStatisticsDto implements Serializable {

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 点赞数（包括数据库和Redis中未同步的事件）
     */
    private Integer likeCount;

    /**
     * 评论数（包括数据库和Redis中未同步的事件）
     */
    private Integer commentCount;

    /**
     * 当前用户是否已点赞（仅当查询时传入userId时才有效）
     */
    private Boolean isLiked;
}
