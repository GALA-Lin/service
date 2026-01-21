package com.unlimited.sports.globox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 治理模块 启动类
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
public class GovernanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(GovernanceApplication.class, args);
    }
}
