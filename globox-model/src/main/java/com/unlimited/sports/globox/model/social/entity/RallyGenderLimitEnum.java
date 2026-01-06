package com.unlimited.sports.globox.model.social.entity;

/**
 * 性别限制枚举
 **/
public enum RallyGenderLimitEnum {
    NO_LIMIT(0, "男女不限"),
    MALE_ONLY(1, "仅男生"),
    FEMALE_ONLY(2, "仅女生");

    private final int code;
    private final String message;

    RallyGenderLimitEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message ;
    }

    public static RallyGenderLimitEnum getByCode(int code) {
        for (RallyGenderLimitEnum genderLimit : values()) {
            if (genderLimit.getCode() == code) {
                return genderLimit;
            }
        }
        throw new IllegalArgumentException("Invalid gender limit code: " + code);
    }
}
