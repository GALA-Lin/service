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
    UNPAID(1, "支付中"),
    PAID(2, "已支付"),
    CLOSED(3, "已关闭/已取消（未支付关闭）"),
    PARTIALLY_REFUNDED(4, "部分退款"),
    FINISH(5, "支付已完成（无法退款）"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
