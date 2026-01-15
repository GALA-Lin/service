package com.unlimited.sports.globox.common.config;

import com.unlimited.sports.globox.common.lock.RedisDistributedLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisLockAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public RedisDistributedLock redisDistributedLock(RedissonClient redissonClient) {
        RedisDistributedLock lock = new RedisDistributedLock();
        lock.setRedissonClient(redissonClient);
        return lock;
    }
}