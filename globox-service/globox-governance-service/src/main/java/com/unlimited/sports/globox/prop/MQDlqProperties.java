package com.unlimited.sports.globox.prop;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "dlq")
public class MQDlqProperties {

    /**
     * 需要监听的最终死信队列列表
     */
    private List<String> queues;

    private int concurrentConsumers;
    private int maxConcurrentConsumers;
    private int prefetch;
}