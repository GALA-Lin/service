package com.unlimited.sports.globox.config;

import com.unlimited.sports.globox.consumer.UnifiedDlqMessageListener;
import com.unlimited.sports.globox.prop.MQDlqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqDynamicRegistrar implements ApplicationListener<EnvironmentChangeEvent> {

    private final RabbitListenerEndpointRegistry registry;
    private final SimpleRabbitListenerContainerFactory dlqContainerFactory;
    private final MQDlqProperties props;
    private final UnifiedDlqMessageListener messageListener;
    private final Environment environment;

    /**
     * queue -> current endpointId
     */
    private final Map<String, String> queueToId = new ConcurrentHashMap<>();

    @PostConstruct
    public void initRegister() {
        reconcile(readQueuesFromEnv());
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        boolean queuesChanged = event.getKeys().stream().anyMatch(k ->
                "dlq.queues".equals(k) || k.startsWith("dlq.queues[")
        );

        if (queuesChanged) {
            List<String> latestQueues = readQueuesFromEnv();
            log.info("[DLQ] queues changed keys={}, latestQueues={}, start reconcile...",
                    event.getKeys(), latestQueues);
            reconcile(latestQueues);
            return;
        }

        boolean otherDlqChanged = event.getKeys().stream().anyMatch(k ->
                k.startsWith("dlq.")
                        && !("dlq.queues".equals(k) || k.startsWith("dlq.queues["))
        );

        if (otherDlqChanged) {
            log.info("[DLQ] dlq config changed (non-queues) keys={}, apply container settings.",
                    event.getKeys());
            applyContainerSettings();
        }
    }

    /**
     * 从 Environment 读取最新 dlq.queues[*]，避免 @ConfigurationProperties 未及时刷新导致拿到旧值
     */
    private List<String> readQueuesFromEnv() {
        List<String> queues = new ArrayList<>();
        for (int i = 0; ; i++) {
            String key = "dlq.queues[" + i + "]";
            String v = environment.getProperty(key);
            if (!StringUtils.hasText(v)) {
                break;
            }
            queues.add(v.trim());
        }
        // 也允许你用逗号字符串形式：dlq.queues=...
        if (queues.isEmpty()) {
            String csv = environment.getProperty("dlq.queues");
            if (StringUtils.hasText(csv)) {
                for (String s : csv.split(",")) {
                    if (StringUtils.hasText(s)) queues.add(s.trim());
                }
            }
        }
        return queues;
    }

    private void applyContainerSettings() {
        int cc = props.getConcurrentConsumers();
        int mcc = props.getMaxConcurrentConsumers();
        int prefetch = props.getPrefetch();

        for (String id : queueToId.values()) {
            var c = registry.getListenerContainer(id);
            if (c instanceof org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer container) {
                container.setConcurrentConsumers(cc);
                container.setMaxConcurrentConsumers(mcc);
                container.setPrefetchCount(prefetch);
                log.info("[DLQ] updated container settings id={}, cc={}, mcc={}, prefetch={}",
                        id, cc, mcc, prefetch);
            }
        }
    }

    private synchronized void reconcile(List<String> newQueuesRaw) {
        Set<String> newQueues = new HashSet<>();
        if (newQueuesRaw != null) {
            for (String q : newQueuesRaw) {
                if (StringUtils.hasText(q)) newQueues.add(q.trim());
            }
        }

        Set<String> oldQueues = new HashSet<>(queueToId.keySet());

        // 1) stop removed
        for (String q : oldQueues) {
            if (!newQueues.contains(q)) {
                String id = queueToId.remove(q);
                if (id != null) {
                    var container = registry.getListenerContainer(id);
                    if (container != null) {
                        container.stop();
                    }
                    // 旧 container 停掉，不注销
                    log.info("[DLQ] stopped listener id={}, queue={}", id, q);
                }
            }
        }

        // 2) register added (用新 id，避免旧 id 残留导致注册失败/不生效)
        for (String q : newQueues) {
            if (queueToId.containsKey(q)) continue;

            String id = buildUniqueId(q);
            SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
            endpoint.setId(id);
            endpoint.setQueueNames(q);
            endpoint.setMessageListener(messageListener);

            registry.registerListenerContainer(endpoint, dlqContainerFactory, true);
            queueToId.put(q, id);

            log.info("[DLQ] registered listener id={}, queue={}", id, q);
        }

        log.info("[DLQ] reconcile done. queues={}", queueToId);
    }

    private String buildUniqueId(String queue) {
        // 关键：每次新注册一个队列，都用新的 id，避免 registry 内部 id 冲突导致“看起来注册了但不消费”
        return "governance-dlq-" + queue + "-" + System.currentTimeMillis();
    }
}