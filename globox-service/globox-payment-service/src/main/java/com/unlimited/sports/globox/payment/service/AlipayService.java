package com.unlimited.sports.globox.payment.service;

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

//    /**
//     * 退款
//     */
//    boolean refund(Long orderId);
//
//    /**
//     * 查询支付宝交易记录状态
//     */
//    Boolean checkPayment(Long orderId);
//
//    /**
//     * 支付宝关闭交易
//     */
//    Boolean closePay(Long orderId);
}
