package com.unlimited.sports.globox.payment.config;

import com.unlimited.sports.globox.payment.prop.WechatPayMoonCourtProperties;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.RSAPublicKeyNotificationConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.refund.RefundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * moonCourt 配置
 */
@Profile("!dev")
@Configuration
public class WechatPayMoonCourtConfig {

    @Autowired
    private WechatPayMoonCourtProperties wechatPayMoonCourtProperties;

    /**
     * moonCourt 支付时的配置
     */
    @Bean
    public Config moonCourtWechatPayClientConfig() throws IOException {
        String apiV3KeyPath = wechatPayMoonCourtProperties.getApiV3Key();

        String apiV3Key = Files.readString(Path.of(apiV3KeyPath), StandardCharsets.UTF_8).trim();
        // 使用微信支付公钥的RSA配置
        return new RSAPublicKeyConfig.Builder()
                .merchantId(wechatPayMoonCourtProperties.getMchid())
                .privateKeyFromPath(wechatPayMoonCourtProperties.getPrivateKey())
                .publicKeyId(wechatPayMoonCourtProperties.getWechatPayPublicKeyId())
                .publicKeyFromPath(wechatPayMoonCourtProperties.getWechatPayPublicKey())
                .merchantSerialNumber(wechatPayMoonCourtProperties.getCertificateSerialNo())
                .apiV3Key(apiV3Key)
                .build();
    }


    /**
     * moonCourt jspoai 支付时的回调配置
     */
    @Bean
    public NotificationConfig moonCourtNotificationConfig() throws IOException {

        String apiV3KeyPath = wechatPayMoonCourtProperties.getApiV3Key();

        String apiV3Key = Files.readString(Path.of(apiV3KeyPath), StandardCharsets.UTF_8).trim();
        return new RSAPublicKeyNotificationConfig.Builder()
                .apiV3Key(apiV3Key)
                .publicKeyId(wechatPayMoonCourtProperties.getWechatPayPublicKeyId())
                .publicKeyFromPath(wechatPayMoonCourtProperties.getWechatPayPublicKey())
                .build();
    }


    /**
     * moonCourt jspoai 支付客户端
     */
    @Bean
    public JsapiServiceExtension moonCourtJsapiService(Config moonCourtWechatPayClientConfig) {
        return new JsapiServiceExtension.Builder().config(moonCourtWechatPayClientConfig).build();
    }


    /**
     * moonCourt jspoai 退款专用客户端
     */
    @Bean
    public RefundService moonCourtRefundService(Config moonCourtWechatPayClientConfig) {
        return new RefundService.Builder().config(moonCourtWechatPayClientConfig).build();
    }

}
