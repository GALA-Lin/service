package com.unlimited.sports.globox.gateway.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * 白名单 - 属性类
 *
 * @author beanmak1r
 * @since 2025/12/19 12:49
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "auth-whitelist")
public class AuthWhitelistProperties {

    /**
     * 鉴权白名单 URL
     */
    private List<String> urls;
}