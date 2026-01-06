package com.unlimited.sports.globox.payment.service.impl;

import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.service.WechatPayJsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
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

    @Override
    public String submit(Payments payments) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(wechatPayProperties.getAppid());
        prepayRequest.setMchid(wechatPayProperties.getMchid());
        prepayRequest.setDescription(payments.getSubject());
        prepayRequest.setOutTradeNo(payments.getOutTradeNo());
        prepayRequest.setTimeExpire(paymentsService.getPaymentTimeout(payments));
        prepayRequest.setNotifyUrl(wechatPayProperties.getNotifyPaymentUrl());
        Amount amount = new Amount();
        amount.setTotal(payments.getTotalAmount()
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact());
        prepayRequest.setAmount(amount);
        PrepayWithRequestPaymentResponse response = jsapiService.prepayWithRequestPayment(prepayRequest);
        log.info("prepay 成功：{}", jsonUtils.objectToJson(response));
//        return response.getPrepayId();
        return "ok";
    }
}
