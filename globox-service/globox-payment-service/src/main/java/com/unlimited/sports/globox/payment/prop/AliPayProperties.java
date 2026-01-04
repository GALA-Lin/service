package com.unlimited.sports.globox.payment.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author makeronbean
 */
@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {
    /**
     * 阿里支付公钥
     */
    private String alipayPublicKey;

    /**
     * 阿里支付请求接口路径
     */
    private String alipayUrl;

    /**
     * 自己应用的id
     */
    private String appId;

    /**
     * 应用私钥
     */
    private String appPrivateKey;

    /**
     * 返回值格式
     */
    private String format;

    /**
     * 字符集编码
     */
    private String charset;

    /**
     * 签名加密方式
     */
    private String signType;

    /**
     * 支付成功后的接口 url
     */
    private String returnPaymentUrl;

    /**
     * 异步通知 url
     */
    private String notifyPaymentUrl;

}
