package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户球风标签关联表
 */
@Data
@TableName("user_style_tag")
public class UserStyleTag {

    /**
     * 用户标签ID（自增主键）
     */
    @TableId(value = "user_style_tag_id", type = IdType.AUTO)
    private Long userStyleTagId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 标签ID
     */
    private Long tagId;

    /**
     * 是否已注销
     */
    private Boolean cancelled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
