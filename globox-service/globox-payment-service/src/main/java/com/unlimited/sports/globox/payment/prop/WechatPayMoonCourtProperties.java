package com.unlimited.sports.globox.payment.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Data
@Component
@Profile("!dev")
@ConfigurationProperties(prefix = "wechat-pay.mooncourt")
public class WechatPayMoonCourtProperties {

    private String apiV3Key;

    /**
     * app id
     */
    private String appid;

    /**
     * 商户号 mchid
     */
    private String mchid;

    /**
     * 商户 API 证书序列号
     */
    private String certificateSerialNo;

    /**
     * 商户 API 证书私钥（PEM 内容或路径）
     */
    private String privateKey;

    /**
     * 微信支付平台公钥 ID
     */
    private String wechatPayPublicKeyId;

    /**
     * 微信支付平台公钥
     */
    private String wechatPayPublicKey;

    /**
     * 支付结果回调地址
     */
    private String notifyPaymentUrl;

    @Data
    public static class Path {

        /**
         * JSAPI 支付接口
         */
        private String jsapi;

        /**
         * APP 支付接口
         */
        private String app;
    }
}