package com.unlimited.sports.globox.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 服务提供应用 DEMO - 启动类
 *
 * @author dk
 * @since 2025/12/18 10:10
 */
@EnableDiscoveryClient
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
public class ProducerApplication {
    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(ProducerApplication.class, args);
        Thread.sleep(1000000);
    }
}
