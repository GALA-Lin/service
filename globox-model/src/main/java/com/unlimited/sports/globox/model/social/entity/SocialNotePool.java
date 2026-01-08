package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 笔记池子实体
 */
@Data
@TableName("social_note_pool")
public class SocialNotePool {

    /**
     * 池子ID
     */
    @TableId(value = "pool_id", type = IdType.AUTO)
    private Long poolId;

    /**
     * 笔记ID（一篇笔记只进一次池子）
     */
    @TableField("note_id")
    private Long noteId;

    /**
     * 池子状态
     */
    @TableField("pool_status")
    private PoolStatus poolStatus;

    /**
     * 权重（管理员优先级）
     */
    private Integer weight;

    /**
     * 随机排序键（1~1000000）
     */
    @TableField("shuffle_key")
    private Integer shuffleKey;

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
     * 池子状态枚举
     */
    public enum PoolStatus {
        ENABLED,
        DISABLED,
        ARCHIVED
    }
}

