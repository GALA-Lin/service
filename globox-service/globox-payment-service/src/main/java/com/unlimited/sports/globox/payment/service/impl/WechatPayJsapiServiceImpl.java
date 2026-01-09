package com.unlimited.sports.globox.payment.service.impl;

import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.model.payment.vo.SubmitResultVo;
import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.service.WechatPayJsapiService;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * JSAPI 微信支付
 */
@Slf4j
@Service
public class WechatPayJsapiServiceImpl implements WechatPayJsapiService {

    @Lazy
    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private JsapiServiceExtension jsapiService;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @Autowired
    private JsonUtils jsonUtils;


    /**
     * 提交支付请求到微信JSAPI支付平台。
     *
     * @param payments 包含支付信息的对象，如订单编号、openID等
     * @return 返回微信支付平台返回的预支付交易会话标识，用于后续调起支付
     */
    @Override
    public SubmitResultVo submit(Payments payments) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(wechatPayProperties.getAppid());
        prepayRequest.setMchid(wechatPayProperties.getMchid());
        prepayRequest.setDescription(payments.getSubject());
        prepayRequest.setOutTradeNo(payments.getOutTradeNo());
        prepayRequest.setTimeExpire(paymentsService.getPaymentTimeout(payments));
        prepayRequest.setNotifyUrl(wechatPayProperties.getNotifyPaymentUrl());
        Payer payer = new Payer();
        // TODO 填充 open id
        payer.setOpenid(payments.getOpenId());
        prepayRequest.setPayer(payer);
        Amount amount = new Amount();
        amount.setTotal(payments.getTotalAmount()
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact());
        prepayRequest.setAmount(amount);
        PrepayWithRequestPaymentResponse response = jsapiService.prepayWithRequestPayment(prepayRequest);
        log.info("prepay 成功：{}", jsonUtils.objectToJson(response));
        return SubmitResultVo.builder()
                .outTradeNo(payments.getOutTradeNo())
                .orderStr(jsonUtils.objectToJson(response))
                .build();
    }


    /**
     * 获取指定支付信息的支付状态。
     *
     * @param payments 包含支付详情的对象，如订单编号、对外业务编号等
     * @return 返回包含支付状态信息的Transaction对象
     */
    @Override
    public Transaction getPaymentStatus(Payments payments) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(wechatPayProperties.getMchid());
        request.setOutTradeNo(payments.getOutTradeNo());
        try {
            return jsapiService.queryOrderByOutTradeNo(request);
        } catch (ServiceException e) {
            // API返回失败, 例如ORDER_NOT_EXISTS
            log.error("code={}, message={}}\n", e.getErrorCode(), e.getErrorMessage());
            log.error("reponse body={}\n", e.getResponseBody());
            throw new GloboxApplicationException(PaymentsCode.PAYMENT_INFO_NOT_EXIST.getCode(), e.getErrorMessage());
        }
    }


    /**
     * 取消指定的支付(未支付)。
     *
     * @param payments 包含支付信息的对象，如订单编号、对外业务编号等
     */
    @Override
    public void cancel(Payments payments) {
        CloseOrderRequest closeRequest = new CloseOrderRequest();
        closeRequest.setMchid(wechatPayProperties.getMchid());
        closeRequest.setOutTradeNo(payments.getOutTradeNo());
        jsapiService.closeOrder(closeRequest);
    }
}
