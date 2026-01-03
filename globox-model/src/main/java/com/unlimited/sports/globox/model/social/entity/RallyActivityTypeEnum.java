package com.unlimited.sports.globox.model.social.entity;

/**
 * 活动类型枚举
 **/
public enum RallyActivityTypeEnum {
    UNLIMITED(0, "不限"),
    SINGLES(1, "单打"),
    DOUBLES(2, "双打");

    private final int code;
    private final String desc;

    RallyActivityTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
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
