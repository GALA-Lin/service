package com.unlimited.sports.globox.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.unlimited.sports.globox.payment.prop.AlipayProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class AlipayConfig {

    @Autowired
    private AlipayProperties alipayProperties;

    /**
     * 注入阿里支付连接对象
     */
    @Bean
    @Profile("dev")
    public AlipayClient alipayDevClient() {
        return new DefaultAlipayClient(alipayProperties.getAlipayUrl(),
                alipayProperties.getAppId(), alipayProperties.getAppPrivateKey(), alipayProperties.getFormat(),
                alipayProperties.getCharset(), alipayProperties.getAlipayPublicKey(), alipayProperties.getSignType());
    }


    /**
     * 注入阿里支付连接对象
     */
    @Bean
    @Profile("!dev")
    public AlipayClient alipayClient() throws IOException {
        String privateKey = Files.readString(Path.of(alipayProperties.getAppPrivateKey()), StandardCharsets.UTF_8)
                .trim();
        return new DefaultAlipayClient(
                alipayProperties.getAlipayUrl(),
                alipayProperties.getAppId(),
                privateKey,
                alipayProperties.getFormat(),
                alipayProperties.getCharset(),
                alipayProperties.getAlipayPublicKey(),
                alipayProperties.getSignType());
    }
}
