package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.wechat.pay.java.service.payments.model.Transaction;

/**
 * JSAPI 微信支付
 */
public interface WechatPayJsapiService {


    /**
     * 提交支付请求到微信JSAPI支付平台。
     *
     * @param payments 包含支付信息的对象，如订单编号、openID等
     * @return 返回微信支付平台返回的预支付交易会话标识，用于后续调起支付
     */
    SubmitResultVo submit(Payments payments);

    /**
     * 获取指定支付信息的支付状态。
     *
     * @param payments 包含支付详情的对象，如订单编号、对外业务编号等
     * @return 返回包含支付状态信息的Transaction对象
     */
    Transaction getPaymentStatus(Payments payments);

    /**
     * 取消指定的支付(未支付)。
     *
     * @param payments 包含支付信息的对象，如订单编号、对外业务编号等
     */
    void cancel(Payments payments);
}
