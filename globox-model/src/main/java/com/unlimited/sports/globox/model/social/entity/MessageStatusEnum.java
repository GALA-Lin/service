package com.unlimited.sports.globox.model.social.entity;

/**
 * 消息状态枚举
 */
public enum MessageStatusEnum {
    
    /**
     * 已读
     */
    READ(1, "已读"),
    
    /**
     * 已发送
     */
    SENT(2, "已发送"),
    
    /**
     * 送达
     */
    DELIVERED(3, "送达"),
    
    /**
     * 已撤回
     */
    RECALLED(4, "已撤回"),
    
    /**
     * 已删除
     */
    DELETED(5, "已删除"),
    
    /**
     * 失败
     */
    FAILED(6, "失败");

    private final int code;
    private final String description;

    MessageStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static MessageStatusEnum fromCode(int code) {
        for (MessageStatusEnum status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的消息状态: " + code);
    }
}
