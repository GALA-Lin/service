package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 支付宝支付 服务类
 */
public interface AlipayService {

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
     * @param payments     支付信息
     * @param refundAmount 本次退款金额
     * @param refundReason 本次退款原因
     * @return 如果退款请求处理成功，则返回true；否则返回false
     */
    ResultCode refund(Payments payments, BigDecimal refundAmount, String refundReason);

    /**
     * 支付宝提交支付
     *
     * @param payments 支付信息
     * @return orderStr
     */
    SubmitResultVo submit(Payments payments);


    /**
     * 查询指定订单号的支付状态。
     *
     * @param outTradeNo 商家订单号，用于查询该订单的支付状态
     * @return GetPaymentStatusVo 包含支付状态信息的对象。如果支付成功、关闭或完成，则分别设置相应的支付状态；如果遇到未知支付状态，则抛出异常。
     * @throws GloboxApplicationException 当请求支付宝失败或者支付状态未知时抛出此异常。
     */
    GetPaymentStatusResultVo getPaymentStatus(String outTradeNo);

    void cancel(Payments payments);
}
