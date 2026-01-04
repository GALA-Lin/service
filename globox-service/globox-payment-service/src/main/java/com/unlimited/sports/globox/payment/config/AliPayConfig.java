package com.unlimited.sports.globox.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.msg.AlipayMsgClient;
import com.alipay.api.msg.MsgHandler;
import com.unlimited.sports.globox.payment.handler.AlipayMsgHandler;
import com.unlimited.sports.globox.payment.prop.AlipayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Autowired
    private AlipayProperties alipayProperties;

    /**
     * 注入阿里支付连接对象
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(alipayProperties.getAlipayUrl(),
                alipayProperties.getAppId(), alipayProperties.getAppPrivateKey(), alipayProperties.getFormat(),
                alipayProperties.getCharset(), alipayProperties.getAlipayPublicKey(), alipayProperties.getSignType());
    }


    /**
     * 注入阿里支付长连接对象
     * TODO 如果测试下来多实例无法建立多个 client 时，采用 http 方式
     */
    @Bean
    public AlipayMsgClient alipayMsgClient() throws Exception {
        // 获取client对象，一个appId对应一个实例
        final AlipayMsgClient alipayMsgClient = AlipayMsgClient.getInstance(alipayProperties.getAppId());
        // 目标支付宝服务端地址
        // 线上环境为 openchannel.alipay.com
        // 沙箱环境为 openchannel-sandbox.dl.alipaydev.com
        alipayMsgClient.setConnector("openchannel-sandbox.dl.alipaydev.com");
        alipayMsgClient.setSecurityConfig(alipayProperties.getSignType(),
                alipayProperties.getAppPrivateKey(),
                alipayProperties.getAlipayPublicKey());
        alipayMsgClient.setMessageHandler(new AlipayMsgHandler());

        return alipayMsgClient;
    }
}
