package com.unlimited.sports.globox;


import com.unlimited.sports.globox.notification.config.TencentCloudProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 推送服务模块 - 启动类
 */

@EnableDiscoveryClient
@EnableConfigurationProperties(TencentCloudProperties.class)
@SpringBootApplication
public class NotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
