package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 笔记实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("social_note")
public class SocialNote {

    /**
     * 笔记ID
     */
    @TableId(value = "note_id", type = IdType.AUTO)
    private Long noteId;

    /**
     * 作者ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 封面图（首图或视频封面）
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 媒体类型（用于快速展示）
     */
    @TableField("media_type")
    private MediaType mediaType;

    /**
     * 是否允许评论
     */
    @TableField("allow_comment")
    private Boolean allowComment;

    /**
     * 点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 评论数
     */
    @TableField("comment_count")
    private Integer commentCount;

    /**
     * 收藏数
     */
    @TableField("collect_count")
    private Integer collectCount;

    /**
     * 是否精选
     */
    @TableField("featured")
    private Boolean featured;

    /**
     * 笔记标签列表 (各个标签之间通过 ; 隔开)
     */
    @TableField("tags")
    private String tags;

    /**
     * 状态
     */
    private Status status;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 媒体类型枚举
     */
    public enum MediaType {
        IMAGE,
        VIDEO
    }

    /**
     * 状态枚举
     */
    public enum Status {
        DRAFT,
        PUBLISHED,
        DELETED
    }
}
