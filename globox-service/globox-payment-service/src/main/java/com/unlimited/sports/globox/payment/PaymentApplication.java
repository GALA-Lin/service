package com.unlimited.sports.globox.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 支付模块 - 启动类
 *
 * @author dk
 * @since 2025/12/21 09:37
 */
@EnableDiscoveryClient
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
public class PaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentApplication.class, args);
    }
}
