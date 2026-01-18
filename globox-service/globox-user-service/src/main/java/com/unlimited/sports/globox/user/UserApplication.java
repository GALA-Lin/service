package com.unlimited.sports.globox.user;

import com.unlimited.sports.globox.user.config.WechatProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * user 模块 - 启动类
 */
@EnableAsync
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
@EnableConfigurationProperties(WechatProperties.class)
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
