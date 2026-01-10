package com.unlimited.sports.globox.common.enums;

import lombok.Getter;

/**
 * 文件类型枚举
 */
@Getter
public enum FileTypeEnum {

    /**
     * 场馆评论图片
     */
    REVIEW_IMAGE(
            "review",
            "评论图片",
            new String[]{".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"},
            10 * 1024 * 1024L  // 10MB
    ),

    /**
     * 用户头像
     */
    AVATAR(
            "avatar",
            "头像",
            new String[]{".jpg", ".jpeg", ".png", ".webp"},
            5 * 1024 * 1024L  // 5MB
    ),

    /**
     * 用户媒体图片
     */
    USER_MEDIA_IMAGE(
            "user-media/image",
            "用户媒体图片",
            new String[]{".jpg", ".jpeg", ".png", ".webp"},
            10 * 1024 * 1024L  // 10MB
    ),

    /**
     * 用户媒体视频
     */
    USER_MEDIA_VIDEO(
            "user-media/video",
            "用户媒体视频",
            new String[]{".mp4", ".mov"},
            100 * 1024 * 1024L  // 100MB
    ),

    /**
     * 笔记图片
     */
    SOCIAL_NOTE_IMAGE(
            "social-note/image",
            "笔记图片",
            new String[]{".jpg", ".jpeg", ".png", ".webp"},
            10 * 1024 * 1024L  // 10MB
    ),

    /**
     * 笔记视频
     */
    SOCIAL_NOTE_VIDEO(
            "social-note/video",
            "笔记视频",
            new String[]{".mp4", ".mov"},
            100 * 1024 * 1024L  // 100MB
    ),

    /**
     * 场馆详情图片
     */
    VENUE_IMAGE(
            "venue-detail",
            "场馆详情图片",
            new String[]{".jpg", ".jpeg", ".png", ".webp"},
            10 * 1024 * 1024L  // 10MB
    ),

    /**
     * 球星卡肖像（抠图后的透明背景图）
     */
    STAR_CARD_PORTRAIT(
            "star-card-portrait",
            "球星卡肖像",
            new String[]{".jpg", ".jpeg", ".png", ".webp"},
            10 * 1024 * 1024L  // 10MB
    );

    /**
     * 类型代码（用于文件路径）
     */
    private final String filePath;

    /**
     * 类型描述
     */
    private final String description;

    /**
     * 允许的文件扩展名
     */
    private final String[] allowedExtensions;

    /**
     * 默认最大文件大小（字节）
     */
    private final Long defaultMaxSize;

    FileTypeEnum(String filePath, String description, String[] allowedExtensions, Long defaultMaxSize) {
        this.filePath = filePath;
        this.description = description;
        this.allowedExtensions = allowedExtensions;
        this.defaultMaxSize = defaultMaxSize;
    }

    /**
     * 根据code获取枚举
     */
    public static FileTypeEnum fromCode(String code) {
        for (FileTypeEnum type : values()) {
            if (type.filePath.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown file type code: " + code);
    }

    /**
     * 判断文件扩展名是否被允许
     */
    public boolean isExtensionAllowed(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String lowerExt = extension.toLowerCase();
        for (String allowed : allowedExtensions) {
            if (lowerExt.equals(allowed)) {
                return true;
            }
        }
        return false;
    }
}
