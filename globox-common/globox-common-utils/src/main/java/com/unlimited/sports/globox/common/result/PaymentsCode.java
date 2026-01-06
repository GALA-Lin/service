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
    PAYMENT_CREATE_FAILED(3003, "支付信息创建失败"),
    PAYMENT_REFUND_FAILED(3304, "退款失败"),

    ORDER_PAID(3305, "订单已支付，无需再次申请支付"),
    NOT_SUPPORTED_PAYMENT_TYPE(3306, "不支持的支付方式"),
    PAYMENT_ALIPAY_FAILED(3307, "请求支付宝创建支付失败"),
    NOT_SUPPORTED_PAYMENT_CLIENT_TYPE(3308, "不支持的客户端");
    private final Integer code;
    private final String message;
}
