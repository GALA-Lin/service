package com.unlimited.sports.globox.social.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * IM 服务 properties
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "im")
public class IMProperties {

    private Long appId;

    private String secretKey;

    private Long userSigExpire;

    private String imServerBaseUrl;

    /**
     * globox 业务系统对接 im 系统的账户
     */
    private String identifier;
}
