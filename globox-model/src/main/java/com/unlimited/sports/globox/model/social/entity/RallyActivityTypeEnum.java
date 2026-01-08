package com.unlimited.sports.globox.model.social.entity;

/**
 * 活动类型枚举
 **/
public enum RallyActivityTypeEnum {
    UNLIMITED(0, "不限"),
    SINGLES(1, "单打"),
    DOUBLES(2, "双打");

    private final int code;
    private final String message;

    RallyActivityTypeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static RallyActivityTypeEnum getByCode(int code) {
        for (RallyActivityTypeEnum type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
