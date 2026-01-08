package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 服务提供方 - 枚举
 */
@Getter
@AllArgsConstructor
public enum SellerTypeEnum {
    VENUE(1, "场馆"),
    COACH(2, "教练"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
