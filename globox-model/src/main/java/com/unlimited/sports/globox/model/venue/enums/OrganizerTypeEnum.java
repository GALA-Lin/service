package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 活动组织者类型枚举
 */
@AllArgsConstructor
@Getter
public enum OrganizerTypeEnum {
    /**
     * 老板/商家所有者（MERCHANT_OWNER）
     */
    OWNER(1, "商家所有者"),

    /**
     * 员工（MERCHANT_STAFF）
     */
    STAFF(2, "员工");

    private final int value;
    private final String description;

    /**
     * 根据值获取枚举
     *
     * @param value 枚举值
     * @return 枚举实例，如果不存在则返回null
     */
    public static OrganizerTypeEnum fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }
}
