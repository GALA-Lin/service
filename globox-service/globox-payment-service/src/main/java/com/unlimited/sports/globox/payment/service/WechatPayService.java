package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.model.payment.entity.Payments;

/**
 * 微信支付 服务类
 */
public interface WechatPayService {

    String submit(Payments payments);

    String handleCallback();
}
