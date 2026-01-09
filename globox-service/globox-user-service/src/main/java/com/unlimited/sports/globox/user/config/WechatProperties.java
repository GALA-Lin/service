package com.unlimited.sports.globox.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 微信服务配置属性
 */
@Data
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {

    /**
     * 微信服务开关：关闭时强制使用Mock模式
     */
    private Boolean enabled = true;

    /**
     * 强制Mock模式开关（优先级最高）
     */
    private Boolean mockEnabled = false;

    /**
     * 小程序配置（用于 third-party-jsapi）
     */
    private MiniappConfig miniapp = new MiniappConfig();

    /**
     * 原生App配置（用于 app）
     */
    private UniappConfig uniapp = new UniappConfig();

    @Data
    public static class MiniappConfig {
        /**
         * 微信API URL
         */
        private String apiUrl = "https://api.weixin.qq.com/sns/jscode2session";

        /**
         * 小程序AppId
         */
        private String appId;

        /**
         * 小程序AppSecret
         */
        private String appSecret;
    }

    @Data
    public static class UniappConfig {
        /**
         * 微信API URL
         */
        private String apiUrl = "https://api.weixin.qq.com/sns/jscode2session";

        /**
         * 原生App AppId
         */
        private String appId;

        /**
         * 原生App AppSecret
         */
        private String appSecret;
    }
}

