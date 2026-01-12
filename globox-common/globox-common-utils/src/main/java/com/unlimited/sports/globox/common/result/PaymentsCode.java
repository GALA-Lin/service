package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付模块错误码枚举
 * 5000-5999
 */
@Getter
@AllArgsConstructor
public enum PaymentsCode implements ResultCode {

    PAYMENT_INFO_NOT_EXIST(5002, "支付信息不存在，请稍后再试"),
    PAYMENT_CREATE_FAILED(5003, "支付信息创建失败，请稍后再试"),
    PAYMENT_REFUND_FAILED(5004, "退款失败，请稍后再试"),
    ORDER_PAID(5005, "订单已支付，无需再次申请支付"),
    NOT_SUPPORTED_PAYMENT_TYPE(5006, "不支持的支付方式，请重新选择支付类型"),
    PAYMENT_ALIPAY_FAILED(5007, "请求支付宝失败，青稍后再试"),
    NOT_SUPPORTED_PAYMENT_CLIENT_TYPE(5008, "不支持的客户端类型"),
    PAYMENT_REFUND_AMOUNT_ERROR(5009, "退款金额有误，请重试"),
    PAYMENT_STATUS_UNKNOW(5010, "未知的支付状态"),
    PAYMENT_REQUEST_PLATFORM_ERROR(5011, "请求支付平台错误，请稍后再试"),
    PAYMENT_WECHAT_PAY_FAILED(5012, "请求微信支付失败，青稍后再试"),
    THIRD_PARTY_TYPE_NOT_EXIST(5013, "第三方小程序不存在");

    private final Integer code;
    private final String message;
}
