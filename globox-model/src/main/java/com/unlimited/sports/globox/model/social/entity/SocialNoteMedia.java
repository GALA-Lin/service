package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记媒体实体
 */
@Data
@TableName("social_note_media")
public class SocialNoteMedia {

    /**
     * 媒体ID
     */
    @TableId(value = "media_id", type = IdType.AUTO)
    private Long mediaId;

    /**
     * 笔记ID
     */
    @TableField("note_id")
    private Long noteId;

    /**
     * 媒体类型
     */
    @TableField("media_type")
    private MediaType mediaType;

    /**
     * 媒体文件URL
     */
    private String url;

    /**
     * 视频封面URL（图片可为空）
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 展示排序
     */
    private Integer sort;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 是否已删除：0-否，1-是
     */
    @TableField("deleted")
    private Boolean deleted;

    /**
     * 媒体类型枚举
     */
    public enum MediaType {
        IMAGE,
        VIDEO
    }
}
