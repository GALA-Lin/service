package com.unlimited.sports.globox.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 订单模块 - 启动类
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
