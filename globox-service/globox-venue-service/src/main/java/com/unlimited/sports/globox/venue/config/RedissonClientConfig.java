package com.unlimited.sports.globox.venue.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson 注入
 * TODO Q： 为什么没有自动注入？
 */
@Configuration
@EnableConfigurationProperties(RedissonClientConfig.RedissonProps.class)
public class RedissonClientConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedissonProps props) {
        Config config = new Config();

        String address = "redis://" + props.getHost() + ":" + props.getPort();
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(props.getDatabase())
                .setPassword(props.getPassword() == null || props.getPassword().isBlank() ? null : props.getPassword());

        return Redisson.create(config);
    }

    @Data
    @ConfigurationProperties(prefix = "spring.redis")
    public static class RedissonProps {
        private String host = "127.0.0.1";
        private Integer port = 6379;
        private Integer database = 0;
        private String password;
    }
}
