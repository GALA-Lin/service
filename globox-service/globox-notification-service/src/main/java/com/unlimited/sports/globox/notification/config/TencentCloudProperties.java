package com.unlimited.sports.globox.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 腾讯云TPNS配置属性
 */
@Data
@ConfigurationProperties(prefix = "tencent.cloud.tpns")
public class TencentCloudProperties {

    /**
     * AppId（应用ID）
     */
    private String appId;

    /**
     * 秘钥（SecretKey）
     */
    private String secretKey;

    /**
     * 服务地址（默认：https://api.tpns.tencent.com/）
     */
    private String domainUrl = "https://api.tpns.tencent.com/";

    /**
     * AccessKey（向后兼容）
     */
    private String accessKey;

    /**
     * 服务地址（向后兼容）
     */
    private String serviceUrl;

    /**
     * 请求超时时间（单位：秒）
     */
    private Integer timeout = 30;

    /**
     * 最大重试次数
     */
    private Integer maxRetries = 3;

    /**
     * 是否启用批量推送
     */
    private Boolean batchPushEnabled = true;

    /**
     * 批量推送的单批数量
     */
    private Integer batchSize = 1000;
}
