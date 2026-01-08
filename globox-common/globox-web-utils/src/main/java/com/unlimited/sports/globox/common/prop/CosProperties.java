package com.unlimited.sports.globox.common.prop;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云COS配置属性
 */
@Data
@Component
@ConditionalOnProperty(
        prefix = "tencent.cos",
        name = {"secret-id", "secret-key", "region", "bucket-name"},
        matchIfMissing = false
)
@ConfigurationProperties(prefix = "tencent.cos")
public class CosProperties {

    /**
     * SecretId
     */
    private String secretId;

    /**
     * SecretKey
     */
    private String secretKey;

    /**
     * 地域
     */
    private String region;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 访问域名
     */
    private String domain;

    /**
     * 上传路径前缀
     */
    private String pathPrefix;
}

