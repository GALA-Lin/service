package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 评论删除操作人类型枚举
 */
@Getter
@AllArgsConstructor
public enum ReviewDeleteOperatorType {

    USER_SELF(1, "用户自己删除"),
    ADMIN_DELETE(2, "管理员删除");

    private final int value;

    private final String description;

    public static ReviewDeleteOperatorType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid ReviewDeleteOperatorType value: " + value));
    }
}
