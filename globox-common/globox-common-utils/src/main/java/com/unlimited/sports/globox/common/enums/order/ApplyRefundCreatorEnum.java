package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 申请退款人枚举
 */
@Getter
@AllArgsConstructor
public enum ApplyRefundCreatorEnum {
    USER(1, "USER"),
    MERCHANT(2, "MERCHANT"),
    ADMIN(3, "ADMIN")
    ;

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
