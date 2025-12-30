package com.unlimited.sports.globox.model.social.entity;

/**
 * 会话类型枚举

 */
public enum ConversationTypeEnum {
    
    /**
     * 私信
     */
    PRIVATE(1, "私信"),
    
    /**
     * 群聊
     */
    GROUP(2, "群聊"),
    
    /**
     * 系统消息
     */
    SYSTEM(3, "系统消息");

    private final int code;
    private final String description;

    ConversationTypeEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ConversationTypeEnum fromCode(int code) {
        for (ConversationTypeEnum type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的会话类型: " + code);
    }
}
