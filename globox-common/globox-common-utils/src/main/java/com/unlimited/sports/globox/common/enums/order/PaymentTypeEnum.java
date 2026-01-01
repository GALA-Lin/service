package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
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
    private final Integer code;
    private final String description;
}
