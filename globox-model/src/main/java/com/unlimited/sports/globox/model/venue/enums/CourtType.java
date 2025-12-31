package com.unlimited.sports.globox.model.venue.enums;

import java.util.Arrays;

public enum CourtType {
    INDOOR(1, "室内"),
    OUTDOOR(2, "室外"),
    SEMI_COVERED(3, "风雨场"),
    SEMI_ENCLOSED(4, "半封闭");

    private final int value;
    private final String description;

    CourtType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static CourtType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }
}
