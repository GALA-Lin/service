package com.unlimited.sports.globox.payment.config;

import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.RSAPublicKeyNotificationConfig;
import com.wechat.pay.java.service.payments.app.AppServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 微信支付注入
 */
@Slf4j
@Profile("!dev")
@Configuration
public class WechatPayConfig {

    @Autowired
    private WechatPayProperties wechatPayProperties;

    /**
     * App 支付时的配置
     */
    @Bean
    public Config wechatPayClientConfig() throws IOException {
        String apiV3KeyPath = wechatPayProperties.getApiV3Key();

        String apiV3Key = Files.readString(Path.of(apiV3KeyPath), StandardCharsets.UTF_8).trim();
        // 使用微信支付公钥的RSA配置
        return new RSAPublicKeyConfig.Builder()
                .merchantId(wechatPayProperties.getMchid())
                .privateKeyFromPath(wechatPayProperties.getPrivateKey())
                .publicKeyId(wechatPayProperties.getWechatPayPublicKeyId())
                .publicKeyFromPath(wechatPayProperties.getWechatPayPublicKey())
                .merchantSerialNumber(wechatPayProperties.getCertificateSerialNo())
                .apiV3Key(apiV3Key)
                .build();
    }

    /**
     * App 支付时的回调配置
     */
    @Bean
    public NotificationConfig notificationConfig() throws IOException {

        String apiV3KeyPath = wechatPayProperties.getApiV3Key();

        String apiV3Key = Files.readString(Path.of(apiV3KeyPath), StandardCharsets.UTF_8).trim();
        return new RSAPublicKeyNotificationConfig.Builder()
                .apiV3Key(apiV3Key)
                .publicKeyId(wechatPayProperties.getWechatPayPublicKeyId())
                .publicKeyFromPath(wechatPayProperties.getWechatPayPublicKey())
                .build();

    }


    /**
     * app 支付客户端
     */
    @Bean
    public AppServiceExtension appService(Config wechatPayClientConfig) {
        return new AppServiceExtension.Builder().config(wechatPayClientConfig).build();
    }


    /**
     * jsapi 支付客户端
     */
    @Bean
    public JsapiServiceExtension jsapiService(Config wechatPayClientConfig) {
        return new JsapiServiceExtension.Builder().config(wechatPayClientConfig).build();
    }


    /**
     * App 支付时退款专用客户端
     */
    @Bean
    public RefundService refundService(Config wechatPayClientConfig) {
        return new RefundService.Builder().config(wechatPayClientConfig).build();
    }
}
