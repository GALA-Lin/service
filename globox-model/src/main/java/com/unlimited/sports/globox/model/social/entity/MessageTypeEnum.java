package com.unlimited.sports.globox.model.social.entity;

/**
 * 消息类型枚举
 */
public enum MessageTypeEnum {
    
    /**
     * 文本消息
     */
    TEXT(1, "文本消息"),
    
    /**
     * 图像消息
     */
    IMAGE(2, "图像消息"),
    
    /**
     * 音频消息
     */
    AUDIO(3, "音频消息"),
    
    /**
     * 视频消息
     */
    VIDEO(4, "视频消息"),
    
    /**
     * 文件消息
     */
    FILE(5, "文件消息"),
    
    /**
     * 地址消息
     */
    LOCATION(6, "地址消息");

    private final int code;
    private final String description;

    MessageTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MessageTypeEnum fromCode(int code) {
        for (MessageTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的消息类型: " + code);
    }
}
