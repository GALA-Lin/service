package com.unlimited.sports.globox.model.coach.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/31 16:50
 * 教练接受的场地类型枚举
 */

@Getter
@AllArgsConstructor
public enum CoachAcceptVenueTypeEnum {
    ALL(0, "都可以"),
    CLAY(1, "红土"),
    GRASS(2, "草地"),
    HARD(3, "硬地");

    private final Integer code;
    private final String description;


    public static CoachAcceptVenueTypeEnum fromValue(Integer code) {
        for (CoachAcceptVenueTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的场地类型: " + code);
    }
}