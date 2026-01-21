package com.unlimited.sports.globox.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 业务线程池注入
 */
@Configuration
public class ThreadExecutorConfig {

    @Bean
    public ExecutorService businessExecutorService() {
        int corePoolSize = 8;
        int maxPoolSize  = 32;
        int queueCapacity = 1000;

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
