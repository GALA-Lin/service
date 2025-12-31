package com.unlimited.sports.globox.model.venue.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 槽位类型枚举
 */
@Getter
@AllArgsConstructor
public enum SlotTypeEnum {
    NORMAL(1, "普通槽位"),
    ACTIVITY(2, "活动槽位");

    private final Integer code;
    private final String desc;

    public static SlotTypeEnum getByCode(Integer code) {
        return code == null ? null : Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static String getDesc(Integer code) {
        SlotTypeEnum type = getByCode(code);
        return type != null ? type.desc : "未知类型";
    }
}
