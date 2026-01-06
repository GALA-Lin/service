package com.unlimited.sports.globox.payment.service;

import com.unlimited.sports.globox.model.payment.entity.Payments;

/**
 * App 微信支付
 */
public interface WechatPayAppService {
    String submit(Payments payments);
}
