package com.unlimited.sports.globox.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 腾讯云IM配置
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TencentCloudImProperties.class)
public class TencentCloudImConfig {

    /**
     * 创建腾讯云IM专用的RestTemplate
     */
    @Bean("tencentImRestTemplate")
    public RestTemplate tencentImRestTemplate(TencentCloudImProperties properties) {
        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(properties.getMaxConnTotal());
        connectionManager.setDefaultMaxPerRoute(properties.getMaxConnPerRoute());

        // 配置请求参数
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(properties.getConnectTimeout())
                .setSocketTimeout(properties.getReadTimeout())
                .setConnectionRequestTimeout(properties.getConnectTimeout())
                .build();

        // 创建HttpClient
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 创建RestTemplate
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        RestTemplate restTemplate = new RestTemplate(factory);

        log.info("腾讯云IM RestTemplate初始化成功: maxConnTotal={}, maxConnPerRoute={}, connectTimeout={}ms, readTimeout={}ms",
                properties.getMaxConnTotal(),
                properties.getMaxConnPerRoute(),
                properties.getConnectTimeout(),
                properties.getReadTimeout());

        return restTemplate;
    }
}
