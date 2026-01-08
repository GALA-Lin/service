package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @since 2025/12/31 11:39
 * 启用状态枚举
 */
@Getter
@AllArgsConstructor
public enum EnableStatusEnum {
    // 启用
    ENABLED(1, "启用"),
    // 禁用
    DISABLED(0, "禁用");

    private final Integer code;
    private final String desc;

    public static EnableStatusEnum EnableStatusfromBoolean(Boolean enabled) {
        return Boolean.TRUE.equals(enabled) ? ENABLED : DISABLED;
    }

    public static EnableStatusEnum getEnumByCode(Integer code) {
        for (EnableStatusEnum e : values()) {
            if (Objects.equals(e.getCode(), code)) {
                return e;
            }
        }
        return null;
    }
}
