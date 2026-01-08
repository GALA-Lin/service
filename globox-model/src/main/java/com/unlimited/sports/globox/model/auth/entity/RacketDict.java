package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 球拍字典表
 */
@Data
@TableName("racket_dict")
public class RacketDict {

    /**
     * 球拍字典ID（自增主键）
     */
    @TableId(value = "racket_id", type = IdType.AUTO)
    private Long racketId;

    /**
     * 父级ID
     */
    private Long parentId;

    /**
     * 层级
     */
    private Level level;

    /**
     * 名称
     */
    private String name;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态
     */
    private RacketStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 层级枚举
     */
    public enum Level {
        BRAND,
        SERIES,
        MODEL
    }

    /**
     * 球拍状态枚举
     */
    public enum RacketStatus {
        ACTIVE,
        DISABLED
    }
}
