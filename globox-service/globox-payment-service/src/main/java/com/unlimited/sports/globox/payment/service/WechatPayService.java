package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.GetPaymentStatusResultVo;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.model.payment.vo.WechatPayNotifyVo;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;

/**
 * 微信支付 服务类
 */
public interface WechatPayService {

    /**
     * 提交支付请求到微信支付平台。
     *
     * @param payments 包含支付信息的对象，如订单编号、openID 等
     * @return 返回微信支付平台返回的预支付交易会话标识，用于后续调起支付
     */
    SubmitResultVo submit(Payments payments);

    WechatPayNotifyVo handleCallback(HttpServletRequest request, NotificationConfig notificationConfig);

    /**
     * 查询指定订单号的微信支付状态。
     *
     * @param outTradeNo 商家订单号，用于查询该订单的支付状态
     * @return GetPaymentStatusVo 包含支付状态信息的对象。如果支付成功、关闭或完成，则分别设置相应的支付状态；如果遇到未知支付状态，则抛出异常。
     * @throws GloboxApplicationException 当请求微信支付失败或者支付状态未知时抛出此异常。
     */
    GetPaymentStatusResultVo getPaymentStatus(String outTradeNo);


    /**
     * 处理用户的退款请求。
     *
     * @param payments     支付信息对象，包含订单编号、对外业务编号等
     * @param refundAmount 本次请求的退款金额
     * @param refundReason 退款原因说明
     * @return 如果退款请求处理成功，则返回true；否则返回false
     */
    ResultCode refund(Payments payments, BigDecimal refundAmount, String refundReason);


    /**
     * 取消指定的支付(未支付)。
     *
     * @param payments 包含支付信息的对象，如订单编号、对外业务编号等
     */
    void cancel(Payments payments);


}
