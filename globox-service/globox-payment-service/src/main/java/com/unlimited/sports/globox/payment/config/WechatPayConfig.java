package com.unlimited.sports.globox.payment.config;

import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信支付注入
 */
@Configuration
public class WechatPayConfig {

    @Autowired
    private WechatPayProperties wechatPayProperties;


    @Bean
    public Config wechatPayConfig() {
        // 使用微信支付公钥的RSA配置
        return new RSAPublicKeyConfig.Builder()
                .merchantId(wechatPayProperties.getMchid())
                .privateKey(wechatPayProperties.getPrivateKey())
//                .privateKeyFromPath(privateKeyPath)
                .publicKey(wechatPayProperties.getWechatPayPublicKey())
//                .publicKeyFromPath(publicKeyPath)
                .publicKeyId(wechatPayProperties.getWechatPayPublicKeyId())
                .merchantSerialNumber(wechatPayProperties.getCertificateSerialNo())
                .apiV3Key(wechatPayProperties.getApiV3Key())
                .build();
    }

    /**
     * app 支付客户端
     */
    @Bean
    public AppServiceExtension appService() {
        return new AppServiceExtension.Builder().config(wechatPayConfig()).build();
    }


    /**
     * jsapi 支付客户端
     */
    @Bean
    public JsapiServiceExtension jsapiService() {
        return new JsapiServiceExtension.Builder().config(wechatPayConfig()).build();
    }


    @Bean
    public NotificationConfig notificationConfig() {
        return new RSAAutoCertificateConfig.Builder()
                //商户号
                .merchantId(wechatPayProperties.getMchid())
                //商户API私钥路径
                .privateKey(wechatPayProperties.getPrivateKey())
//                .privateKeyFromPath("F:\\wxpay\\apiclient_key.pem")
                //商户证书序列号
                .merchantSerialNumber(wechatPayProperties.getCertificateSerialNo())
                //商户APIV3密钥
                .apiV3Key(wechatPayProperties.getApiV3Key())
                .build();

    }
}
