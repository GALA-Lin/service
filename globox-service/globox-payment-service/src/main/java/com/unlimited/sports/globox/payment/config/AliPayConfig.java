package com.unlimited.sports.globox.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.unlimited.sports.globox.payment.prop.AliPayProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliPayConfig {

    @Autowired
    private AliPayProperties aliPayProperties;


    /**
     * 注入阿里支付连接对象
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(aliPayProperties.getAlipayUrl(),
                aliPayProperties.getAppId(), aliPayProperties.getAppPrivateKey(),aliPayProperties.getFormat(),
                aliPayProperties.getCharset(),aliPayProperties.getAlipayPublicKey(),aliPayProperties.getSignType());
    }
}
