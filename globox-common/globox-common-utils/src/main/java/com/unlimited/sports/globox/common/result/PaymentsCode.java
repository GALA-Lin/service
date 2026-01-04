package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付模块错误码枚举
 */
@Getter
@AllArgsConstructor
public enum PaymentsCode implements ResultCode {


    PAYMENT_SAVE_FAILED(3001, "支付信息保存失败"),
    PAYMENT_INFO_NOT_EXIST(3002, "支付信息不存在"),
    PAYMENT_INFO_CREATE_FAILED(3003, "支付信息创建失败"),
    PAYMENT_REFUND_FAILED(3304, "退款失败"),

    ;
    private final Integer code;
    private final String message;
}
