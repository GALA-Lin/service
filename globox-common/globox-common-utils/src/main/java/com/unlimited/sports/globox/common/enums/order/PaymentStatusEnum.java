package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付状态枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentStatusEnum {
    UNPAID(1, "UNPAID"),
    PAID(2, "PAID"),
    ;
    @EnumValue
    private final Integer code;
    private final String description;
}
