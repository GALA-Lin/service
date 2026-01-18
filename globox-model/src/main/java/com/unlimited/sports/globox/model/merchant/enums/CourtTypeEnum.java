package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025-12-18-13:37
 * 场地类型枚举
 */
@Getter
@AllArgsConstructor
public enum CourtTypeEnum {

    INDOOR(1, "室内"),
    OUTDOOR(2, "室外"),
    COVERED(3, "风雨场"),
    SEMI_ENCLOSED(4, "其他");

    private final Integer code;
    private final String name;

    public static CourtTypeEnum getByCode(Integer code) {
        for (CourtTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
