package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.model.payment.entity.Payments;

/**
 * JSAPI 微信支付
 */
public interface WechatPayJsapiService {


    String submit(Payments payments);
}
