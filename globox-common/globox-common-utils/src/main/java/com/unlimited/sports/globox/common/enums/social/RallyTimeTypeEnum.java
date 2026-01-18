package com.unlimited.sports.globox.common.enums.social;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 约球时段枚举
 */
@Getter
@AllArgsConstructor
public enum RallyTimeTypeEnum {

    MORNING(0, "上午"),
    AFTERNOON(1,"下午"),
    EVENING(2,"晚上")
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;


    public static RallyTimeTypeEnum fromCode(Integer code) {
        for (RallyTimeTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
