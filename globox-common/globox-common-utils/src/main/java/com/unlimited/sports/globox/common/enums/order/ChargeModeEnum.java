package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 额外费用计费方式 - 枚举
 * 计费方式：1=FIXED，2=PERCENTAGE
 */
@Getter
@AllArgsConstructor
public enum ChargeModeEnum {

    FIXED(1, "FIXED"),
    PERCENTAGE(2, "PERCENTAGE"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
