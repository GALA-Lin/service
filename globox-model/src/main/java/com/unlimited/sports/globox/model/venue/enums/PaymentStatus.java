package com.unlimited.sports.globox.model.venue.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 支付状态枚举
 */
@Getter
public enum PaymentStatus {
    UNPAID(1, "待支付"),
    PAID(2, "已支付"),
    REFUNDING(3, "退款中"),
    REFUNDED(4, "已退款");

    private final int value;
    private final String description;

    PaymentStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public static PaymentStatus fromValue(int value) {
        return Arrays.stream(values())
                .filter(status -> status.value == value)
                .findFirst()
                .orElse(null);
    }
}
