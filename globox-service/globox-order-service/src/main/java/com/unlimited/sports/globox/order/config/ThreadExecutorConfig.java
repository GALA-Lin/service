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
        return new ThreadPoolExecutor(
                5,
                10,
                5,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(100));
    }
}
