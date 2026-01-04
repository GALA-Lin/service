package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.common.message.order.UserRefundMessage;

import java.util.Map;

/**
 * 支付宝支付 服务类
 */
public interface AlipayService {

    /**
     * 提交订单至支付宝进行支付处理。
     *
     * @param orderId 订单ID
     * @return 支付宝返回的处理结果信息，通常为 "success" 或者具体的错误信息
     */
    String submit(Long orderId);

    /**
     * 校验并处理支付宝异步回调请求。
     *
     * @param paramsMap 回调参数映射，包含从支付宝返回的所有参数
     * @return 处理结果信息，通常为 "success" 或者具体的错误信息
     */
    String checkCallback(Map<String, String> paramsMap);


    /**
     * 处理用户的退款请求。
     *
     * @param message 包含退款信息的消息对象，其中包括订单号(orderNo)、外部交易号(outTradeNo)、外部退款请求号(outRequestNo)、退款金额(refundAmount)以及退款原因(refundReason)
     * @return 如果退款请求处理成功，则返回true；否则返回false
     */
    boolean refund(UserRefundMessage message);
}
