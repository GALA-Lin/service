package com.unlimited.sports.globox.venue.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 第三方平台HTTP配置
 */
@Slf4j
@Configuration
public class ThirdPartyPlatformHttpConfig {

    /**
     * 创建第三方平台专用的RestTemplate
     * 支持Cookie自动管理（用于wefitos等基于Cookie认证的平台）
     */
    @Bean("thirdPartyRestTemplate")
    public RestTemplate thirdPartyRestTemplate() {
        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(50);

        // 配置请求参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(5000)
                .build();

        // 创建HttpClient（禁用自动Cookie管理，由代码手动控制Cookie）
        // 不使用setDefaultCookieStore，避免多场馆Cookie冲突
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableCookieManagement()  // 禁用自动Cookie管理
                .build();

        // 创建RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(factory);

        log.info("第三方平台 RestTemplate初始化成功: maxConnTotal=200, maxConnPerRoute=50, connectTimeout=5000ms, socketTimeout=10000ms, cookieManagement=disabled(手动控制)");

        return restTemplate;
    }
}
