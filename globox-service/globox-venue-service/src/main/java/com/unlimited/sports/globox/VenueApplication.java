package com.unlimited.sports.globox;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Venue 模块 - 启动类
 *
 * @author dk
 * @since 2025/12/19 10:17
 */

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.unlimited.sports.globox")
@MapperScan("com.unlimited.sports.globox.**.mapper")
public class VenueApplication {

    public static void main(String[] args) {
        SpringApplication.run(VenueApplication.class, args);


    }
}
