package com.unlimited.sports.globox.model.venue.enums;

import java.util.Arrays;

public enum RuleType {
    REGULAR(1, "常规营业时间"),
    SPECIAL_DATE(2, "特殊日期"),
    CLOSED_DATE(3, "闭馆日期");

    private final int value;
    private final String description;

    RuleType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static RuleType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }
}
