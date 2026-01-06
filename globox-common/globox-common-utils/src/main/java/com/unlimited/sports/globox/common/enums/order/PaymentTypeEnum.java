package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付类型 - 枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentTypeEnum {

    NONE(0, "未选择"),
    WECHAT_PAY(1, "微信支付"),
    ALIPAY(2, "支付宝"),
    ;

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    /**
     * 根据 code 获取支付类型
     *
     * @param code 支付类型编码
     * @return 对应的 PaymentTypeEnum
     */
    public static PaymentTypeEnum from(Integer code) {
        if (code == null) {
            return NONE;
        }

        for (PaymentTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown PaymentTypeEnum code: " + code);
    }
}
