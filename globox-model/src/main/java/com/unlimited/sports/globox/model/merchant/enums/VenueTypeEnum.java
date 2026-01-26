package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

/**
 * 场馆类型枚举
 * 用于区分自有场馆（Home）和第三方集成场馆（Away）
 */
@Getter
@AllArgsConstructor
public enum VenueTypeEnum {
    // 自有场馆
    HOME(1, "自有"),
    // 第三方集成场馆
    AWAY(2, "第三方");

    private final Integer code;
    private final String desc;

    public static VenueTypeEnum getEnumByCode(Integer code) {
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.getCode(), code))
                .findFirst()
                .orElse(null);
    }

    public boolean isAway() {
        return Objects.equals(this.code, AWAY.code);
    }

    public boolean isHome() {
        return Objects.equals(this.code, HOME.code);
    }
}
