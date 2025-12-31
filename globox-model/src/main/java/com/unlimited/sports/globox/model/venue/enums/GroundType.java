package com.unlimited.sports.globox.model.venue.enums;

import java.util.Arrays;

public enum GroundType {
    HARD(1, "硬地"),
    CLAY(2, "红土"),
    GRASS(3, "草地"),
    OTHER(4, "其他");

    private final int value;
    private final String description;

    GroundType(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    public static GroundType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }
}
