package com.unlimited.sports.globox.search;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Search 模块 - 启动类
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
@EnableDiscoveryClient
public class SearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchApplication.class, args);
        log.info("globox search server start successfully!");
    }
}
