package com.unlimited.sports.globox.payment.service.impl;

import com.unlimited.sports.globox.common.utils.JsonUtils;
import com.unlimited.sports.globox.model.payment.entity.Payments;
import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import com.unlimited.sports.globox.payment.service.WechatPayAppService;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.app.model.Amount;
import com.wechat.pay.java.service.payments.app.model.PrepayRequest;
import com.wechat.pay.java.service.payments.app.model.PrepayWithRequestPaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;

/**
 * App 微信支付
 */
@Slf4j
@Service
public class WechatPayAppServiceImpl implements WechatPayAppService {

    @Lazy
    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private WechatPayProperties wechatPayProperties;

    @Autowired
    private AppServiceExtension appService;

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
        PrepayWithRequestPaymentResponse response = appService.prepayWithRequestPayment(prepayRequest);
        log.info("prepay 成功：{}", jsonUtils.objectToJson(response));
        return response.getPrepayId();
    }
}
