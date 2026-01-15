package com.unlimited.sports.globox.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * 市枚举；
 */
@Getter
@AllArgsConstructor
public enum RegionCityEnum implements Serializable {
    CHENG_DU(510100, "成都市"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
