package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperatorTypeEnum {
    USER(1, "USER"),
    MERCHANT(2, "MERCHANT"),
    SYSTEM(3, "SYSTEM");

    @EnumValue
    @JsonValue
    private final int code;
    private final String operatorTypeName;
}