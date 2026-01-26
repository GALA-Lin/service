package com.unlimited.sports.globox.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 业务线程池注入
 */
@Configuration
public class ThreadPoolExecutorConfig {

    /**
     * 业务线程池注册
     */
    @Bean
    public ExecutorService businessExecutorService() {
        return new ThreadPoolExecutor(
                20,
                100,
                5,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(500),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
