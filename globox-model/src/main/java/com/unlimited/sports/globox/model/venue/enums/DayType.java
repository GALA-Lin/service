package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 日期类型枚举
 */
@AllArgsConstructor
@Getter
public enum DayType {
    WEEKDAY(1, "工作日"),
    WEEKEND(2, "周末"),
    HOLIDAY(3, "节假日");

    private final int value;
    private final String description;

    public static DayType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }
}
