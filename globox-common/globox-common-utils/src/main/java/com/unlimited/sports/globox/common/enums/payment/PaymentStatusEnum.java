package com.unlimited.sports.globox.common.enums.payment;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态 - 枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentStatusEnum {
    UNPAID(1,"支付中"),
    PAID(2,"已支付"),

    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
