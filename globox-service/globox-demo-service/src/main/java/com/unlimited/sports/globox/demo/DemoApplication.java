package com.unlimited.sports.globox.demo;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * main
 *
 * @author dk
 * @since 2025/12/17 16:43
 */
@EnableDiscoveryClient
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
public class DemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}