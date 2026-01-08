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
    PAYMENT_REFUND_FAILED(3004, "退款失败"),
    ORDER_PAID(3005, "订单已支付，无需再次申请支付"),
    NOT_SUPPORTED_PAYMENT_TYPE(3006, "不支持的支付方式"),
    PAYMENT_ALIPAY_FAILED(3007, "请求支付宝失败，青稍后再试"),
    NOT_SUPPORTED_PAYMENT_CLIENT_TYPE(3008, "不支持的客户端类型"),
    PAYMENT_REFUND_AMOUNT_ERROR(3009, "退款金额有误，请重试"),
    PAYMENT_STATUS_UNKNOW(3010, "未知的支付状态"),
    PAYMENT_REQUEST_PLATFORM_ERROR(3011, "请求支付平台错误，请稍后再试"),
    PAYMENT_WECHAT_PAY_FAILED(3012, "请求微信支付失败，青稍后再试"),
    ;

    private final Integer code;
    private final String message;
}
