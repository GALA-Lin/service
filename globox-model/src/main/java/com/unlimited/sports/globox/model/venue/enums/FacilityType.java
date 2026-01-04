package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 便利设施枚举
 * 用于在搜索时根据设施进行筛选
 * value为设施ID，用于前端传参和数据库查询
 */
@Getter
@AllArgsConstructor
public enum FacilityType {
    PARKING(1, "停车场"),
    CHANGING_ROOM(2, "更衣室"),
    STRING_MACHINE(3, "穿线机");

    private final int value;
    private final String description;

    public static FacilityType fromValue(int value) {
        return Arrays.stream(values())
                .filter(type -> type.value == value)
                .findFirst()
                .orElse(null);
    }
}
