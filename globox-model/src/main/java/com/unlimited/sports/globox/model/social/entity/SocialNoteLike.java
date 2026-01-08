package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记点赞实体
 */
@Data
@TableName("social_note_like")
public class SocialNoteLike {

    /**
     * 点赞ID
     */
    @TableId(value = "like_id", type = IdType.AUTO)
    private Long likeId;

    /**
     * 笔记ID
     */
    @TableField("note_id")
    private Long noteId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 点赞时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}

