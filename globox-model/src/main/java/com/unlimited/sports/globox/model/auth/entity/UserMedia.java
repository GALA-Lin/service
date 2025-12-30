package com.unlimited.sports.globox.model.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户展示墙媒体表
 */
@Data
@TableName("user_media")
public class UserMedia {

    /**
     * 媒体ID（自增主键）
     */
    @TableId(value = "media_id", type = IdType.AUTO)
    private Long mediaId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 媒体类型
     */
    private MediaType mediaType;

    /**
     * 媒体文件URL
     */
    private String url;

    /**
     * 视频封面URL（图片可为空）
     */
    private String coverUrl;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 展示排序
     */
    private Integer sort;

    /**
     * 状态
     */
    private Status status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
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
        ACTIVE,
        DISABLED
    }
}

