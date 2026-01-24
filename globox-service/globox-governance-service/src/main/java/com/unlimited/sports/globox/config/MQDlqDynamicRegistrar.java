package com.unlimited.sports.globox.config;

import com.unlimited.sports.globox.consumer.UnifiedDlqMessageListener;
import com.unlimited.sports.globox.prop.MQDlqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
//@Component
@RequiredArgsConstructor
public class MQDlqDynamicRegistrar implements SmartLifecycle {

    private final RabbitListenerEndpointRegistry registry;
    private final SimpleRabbitListenerContainerFactory dlqContainerFactory; // @Bean(name="dlqContainerFactory")
    private final MQDlqProperties props;
    private final UnifiedDlqMessageListener messageListener;

    private volatile boolean running = false;
    private final Set<String> registeredIds = new HashSet<>();

    @Override
    public void start() {
        List<String> queues = props.getQueues();
        if (queues == null || queues.isEmpty()) {
            log.warn("[DLQ] no queues configured, skip register.");
            running = true;
            return;
        }

        for (String q : queues) {
            if (!StringUtils.hasText(q)) continue;

            String id = "governance-dlq-" + q;
            if (registeredIds.contains(id)) continue;

            SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
            endpoint.setId(id);
            endpoint.setQueueNames(q);
            endpoint.setMessageListener(messageListener);

            // 第三个参数 true 表示立刻启动容器
            registry.registerListenerContainer(endpoint, dlqContainerFactory, true);
            registeredIds.add(id);

            log.info("[DLQ] registered listener, id={}, queue={}", id, q);
        }

        running = true;
    }

    @Override
    public void stop() {
        // 关闭已注册容器
        for (String id : registeredIds) {
            var container = registry.getListenerContainer(id);
            if (container instanceof SimpleMessageListenerContainer c) {
                c.stop();
            } else {
                container.stop();
            }
        }
        registeredIds.clear();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * 尽量晚启动，确保 ConnectionFactory 等都 ready
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}