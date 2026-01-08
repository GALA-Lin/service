package com.unlimited.sports.globox.model.venue.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 预订槽位状态枚举
 */
@Getter
public enum BookingSlotStatus {
    AVAILABLE(1, "可预订"),
    LOCKED_IN(2, "锁定中"),
    EXPIRED(3, "不可预定");

    private final int value;
    private final String description;

    BookingSlotStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static BookingSlotStatus fromValue(int value) {
        return Arrays.stream(values())
                .filter(status -> status.value == value)
                .findFirst()
                .orElse(null);
    }
}
