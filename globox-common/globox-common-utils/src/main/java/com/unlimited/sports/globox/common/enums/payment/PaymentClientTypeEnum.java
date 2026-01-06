package com.unlimited.sports.globox.common.enums.payment;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付时客户端枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentClientTypeEnum {
    APP(1, "APP"),
    JSAPI(2, "JSAPI"),
    ;

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    /**
     * 根据 code 获取枚举值
     *
     * @param code code
     * @return 对应的 Enum
     */
    public static PaymentClientTypeEnum from(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("Unknown PaymentTypeEnum code: " + code);
        }

        for (PaymentClientTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown PaymentTypeEnum code: " + code);
    }
}
