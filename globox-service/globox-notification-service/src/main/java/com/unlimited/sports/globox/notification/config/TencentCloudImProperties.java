package com.unlimited.sports.globox.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 腾讯云IM配置属性
 */
@Data
@ConfigurationProperties(prefix = "tencent.cloud.im")
public class TencentCloudImProperties {

    /**
     * SDK应用ID
     * 配置中心配置项: tencent.cloud.im.sdk-app-id
     */
    private Long sdkAppId;

    /**
     * 密钥
     * 配置中心配置项: tencent.cloud.im.secret-key
     */
    private String secretKey;

    /**
     * 管理员账号（默认admin）
     * 配置中心配置项: tencent.cloud.im.admin-account
     */
    private String adminAccount = "admin";

    /**
     * API域名
     * 配置中心配置项: tencent.cloud.im.api-domain
     *
     * 根据SDKAppID所在地区选择：
     * - 中国: console.tim.qq.com
     * - 新加坡: adminapisgp.im.qcloud.com
     * - 首尔: adminapikr.im.qcloud.com
     * - 法兰克福: adminapiger.im.qcloud.com
     * - 硅谷: adminapiusa.im.qcloud.com
     * - 雅加达: adminapiidn.im.qcloud.com
     */
    private String apiDomain = "console.tim.qq.com";

    /**
     * UserSig有效期（秒，默认60天）
     * 配置中心配置项: tencent.cloud.im.user-sig-expire
     */
    private Integer userSigExpire = 5184000;

    /**
     * HTTP连接超时时间（毫秒，默认10秒）
     * 配置中心配置项: tencent.cloud.im.connect-timeout
     */
    private Integer connectTimeout = 10000;

    /**
     * HTTP读取超时时间（毫秒，默认30秒）
     * 配置中心配置项: tencent.cloud.im.read-timeout
     */
    private Integer readTimeout = 30000;

    /**
     * HTTP连接池最大连接数（默认200）
     * 配置中心配置项: tencent.cloud.im.max-conn-total
     */
    private Integer maxConnTotal = 200;

    /**
     * HTTP连接池每个路由的最大连接数（默认50）
     * 配置中心配置项: tencent.cloud.im.max-conn-per-route
     */
    private Integer maxConnPerRoute = 50;
}
