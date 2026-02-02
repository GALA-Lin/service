package com.unlimited.sports.globox.social;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
* 社交模块 - 启动类
*
* @author dk
* @since 2025/12/21 10:49
*/
@EnableCaching
@EnableTransactionManagement
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
@MapperScan("com.unlimited.sports.globox.social.mapper")
@EnableDiscoveryClient
public class SocialApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
