package com.unlimited.sports.globox.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 雪花算法
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "id-generator",
        name = "enabled",
        havingValue = "true"
)
public class IdGenerator {

    /**
     * Snowflake epoch
     * 2025-01-01 00:00:00 UTC+8
     */
    private static final long START_TIMESTAMP = 1735660800000L;

    /**
     * 最大允许的时钟回拨（毫秒）
     */
    private static final long MAX_CLOCK_BACKWARD_MS = 10L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long datacenterId;

    /**
     * 上一次发号使用的逻辑时间
     */
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    /**
     * 配置机器ID和数据中心ID
     *
     * @param workerId     机器ID
     * @param datacenterId 数据中心ID
     */
    public IdGenerator(
            @Value("${id-generator.worker-id}") long workerId,
            @Value("${id-generator.datacenter-id}") long datacenterId
    ) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId out of range");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个 ID（线程安全）
     */
    public synchronized long nextId() {
        long currentTimestamp = currentTimeMillis();

        // === 时钟回拨处理 ===
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;

            if (offset <= MAX_CLOCK_BACKWARD_MS) {
                // 小回拨：使用 lastTimestamp 继续发号
                currentTimestamp = lastTimestamp;
            } else {
                // 大回拨：拒绝发号（必须报警）
                log.error("Snowflake clock rollback, offset={}ms, workerId={}, dcId={}", offset, workerId, datacenterId);
                throw new RuntimeException("Clock moved backwards too much: " + offset + "ms");
            }
        }

        // 同一毫秒内
        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long ts;
        do {
            ts = currentTimeMillis();
        } while (ts <= lastTimestamp);
        return ts;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}