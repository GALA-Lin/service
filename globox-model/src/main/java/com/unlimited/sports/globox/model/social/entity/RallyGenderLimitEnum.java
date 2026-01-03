package com.unlimited.sports.globox.model.social.entity;

/**
 * 性别限制枚举
 **/
public enum RallyGenderLimitEnum {
    NO_LIMIT(0, "不限"),
    MALE_ONLY(1, "仅男生"),
    FEMALE_ONLY(2, "仅女生");

    private final int code;
    private final String description;

    RallyGenderLimitEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static RallyGenderLimitEnum fromCode(int code) {
        for (RallyGenderLimitEnum genderLimit : values()) {
            if (genderLimit.getCode() == code) {
                return genderLimit;
            }
        }
        throw new IllegalArgumentException("Invalid gender limit code: " + code);
    }
}
