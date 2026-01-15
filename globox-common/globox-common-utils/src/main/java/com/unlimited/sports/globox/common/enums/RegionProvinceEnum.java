package com.unlimited.sports.globox.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

/**
 * 省枚举；
 */
@Getter
@AllArgsConstructor
public enum RegionProvinceEnum implements Serializable {
    SI_CHUAN(510000, "四川省"),

    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
