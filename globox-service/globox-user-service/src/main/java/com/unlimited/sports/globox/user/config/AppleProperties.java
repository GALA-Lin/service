package com.unlimited.sports.globox.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Apple登录配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "apple")
public class AppleProperties {

    /**
     * 是否启用Apple登录
     */
    private Boolean enabled = false;

    /**
     * 是否启用Mock模式（本地开发用）
     */
    private Boolean mockEnabled = false;

    /**
     * Apple客户端ID（App Bundle ID）
     */
    private String clientId;

    /**
     * Apple服务ID（Service ID）
     */
    private String serviceId;

    /**
     * Apple公钥URL（用于验证JWT签名）
     * 默认：https://appleid.apple.com/auth/keys
     */
    private String publicKeyUrl = "https://appleid.apple.com/auth/keys";

    /**
     * Apple Issuer（JWT验证用）
     * 默认：https://appleid.apple.com
     */
    private String issuer = "https://appleid.apple.com";
}

