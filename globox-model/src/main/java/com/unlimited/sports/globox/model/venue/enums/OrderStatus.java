package com.unlimited.sports.globox.model.venue.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态枚举
 */
@Getter
public enum OrderStatus {
    PENDING(1, "待确认"),
    CONFIRMED(2, "已确认"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    private final int value;
    private final String description;

    OrderStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static OrderStatus fromValue(int value) {
        return Arrays.stream(values())
                .filter(status -> status.value == value)
                .findFirst()
                .orElse(null);
    }
}
