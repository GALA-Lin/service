package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025-12-18-13:38
 * 土地类型枚举
 */
@Getter
@AllArgsConstructor
public enum GroundTypeEnum {

    HARD_COURT(1, "硬地"),
    CLAY_COURT(2, "红土"),
    GRASS_COURT(3, "草地"),
    OTHER(4, "其他");

    private final Integer code;
    private final String name;

    public static GroundTypeEnum getByCode(Integer code) {
        for (GroundTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}

