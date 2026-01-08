package com.unlimited.sports.globox.model.social.entity;

public enum TimeRangeType {
    HALF_DAY(1, "半天内"),
    ONE_DAY(2, "一天内"),
    TWO_DAYS(3, "两天内"),
    FIVE_DAYS(4, "五天内");

    private final int code;
    private final String description;

    TimeRangeType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
    public static TimeRangeType getByCode(int code) {
        for (TimeRangeType value : TimeRangeType.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return null;
    }
}
