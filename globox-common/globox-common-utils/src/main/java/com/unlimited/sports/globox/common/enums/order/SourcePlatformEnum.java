package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 平台来源枚举
 */
@Getter
@AllArgsConstructor
public enum SourcePlatformEnum {

    HOME(1,"HOME 球场"),
    AWAY(2, "AWAY 球场"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;


    public static SourcePlatformEnum from(Integer code) {
        for (SourcePlatformEnum sourcePlatformEnum : SourcePlatformEnum.values()) {
            if (sourcePlatformEnum.getCode().equals(code)) {
                return sourcePlatformEnum;
            }
        }
        return null;
    }

}
