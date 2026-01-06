package com.unlimited.sports.globox.model.social.entity;

/**
 * 时间类型枚举
 **/
public enum RallyTimeTypeEnum {
    MORNING(0, "上午"),
    AFTERNOON(1, "下午"),
    EVENING(2, "晚上");
    private final int code;
    private final String message;
    RallyTimeTypeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
    public int getCode() {
        return code;
    }
    public String getMessage() {
        return message;
    }
    public static RallyTimeTypeEnum getByCode(int code) {
        for (RallyTimeTypeEnum type : values()) {
            if (type.getCode() == code) {
                return type;
            }
            return null;
        }
        return null;
    }
}
