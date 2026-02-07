package com.unlimited.sports.globox.user.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "sms.tencent")
public class TencentSmsProperties {
    private String secretId;
    private String secretKey;
    private String smsSdkAppId;
    private String signName;
    private String templateId;
}