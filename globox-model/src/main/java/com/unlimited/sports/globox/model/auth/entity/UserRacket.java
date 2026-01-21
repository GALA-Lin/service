package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User racket relation.
 */
@Data
@TableName("user_racket")
public class UserRacket {

    /**
     * 用户球拍ID（自增主键）
     */
    @TableId(value = "user_racket_id", type = IdType.AUTO)
    private Long userRacketId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 球拍型号ID
     */
    private Long racketModelId;

    /**
     * 是否主力拍
     */
    private Boolean isPrimary;

    /**
     * 是否已删除
     */
    private Boolean deleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
