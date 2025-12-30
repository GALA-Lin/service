package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单来源枚举
 */
@AllArgsConstructor
@Getter
public enum OrderSource {
    USER(1, "用户端"),
    MERCHANT(2, "商家端");

    private final int value;
    private final String description;

    public static OrderSource fromValue(int value) {
        for (OrderSource source : values()) {
            if (source.value == value) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown OrderSource value: " + value);
    }
}
