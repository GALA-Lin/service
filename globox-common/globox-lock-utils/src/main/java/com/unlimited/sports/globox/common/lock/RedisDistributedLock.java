package com.unlimited.sports.globox.common.lock;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 * 支持MultiLock批量加锁，避免死锁
 */
@Slf4j
@Component
public class RedisDistributedLock {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 批量获取多个锁（逐个加锁，确保所有锁都获取成功）
     * 内部自动排序避免死锁
     *
     * @param lockKeys 锁键列表
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param timeUnit 时间单位
     * @return 所有锁都获取成功返回锁列表，否则返回null
     */
    public List<RLock> tryLockMultiple(List<String> lockKeys, long waitTime, long leaseTime, TimeUnit timeUnit) {
        if (lockKeys == null || lockKeys.isEmpty()) {
            log.warn("锁键列表为空");
            return null;
        }

        // 去重并排序，避免死锁
        List<String> sortedKeys = lockKeys.stream()
                .distinct()
                .sorted()
                .toList();

        log.debug("开始批量获取锁（逐个加锁），共{}个锁，已排序，锁键: {}", sortedKeys.size(), sortedKeys);

        List<RLock> acquiredLocks = new java.util.ArrayList<>();

        try {
            // 逐个获取锁，确保所有锁都必须成功
            for (String key : sortedKeys) {
                RLock lock = redissonClient.getLock(key);
                boolean lockAcquired = lock.tryLock(waitTime, leaseTime, timeUnit);

                if (lockAcquired) {
                    acquiredLocks.add(lock);
                    log.info("成功获取锁: {}, 锁是否被当前线程持有: {}", key, lock.isHeldByCurrentThread());
                } else {
                    // 任何一个锁获取失败，释放已获取的所有锁
                    log.warn("获取锁失败: {}，释放已获取的{}个锁", key, acquiredLocks.size());
                    unlockMultiple(acquiredLocks);
                    return null;
                }
            }

            log.info("成功获取所有锁，共{}个，等待时间: {} {}，锁持有时间: {} {}，锁键: {}",
                    acquiredLocks.size(), waitTime, timeUnit, leaseTime, timeUnit, sortedKeys);
            return acquiredLocks;

        } catch (InterruptedException e) {
            log.error("获取锁被中断", e);
            Thread.currentThread().interrupt();
            unlockMultiple(acquiredLocks);
            return null;
        } catch (Exception e) {
            log.error("获取锁异常", e);
            unlockMultiple(acquiredLocks);
            return null;
        }
    }

    /**
     * 释放多个锁
     *
     * @param locks 锁列表
     */
    public void unlockMultiple(List<RLock> locks) {
        if (locks == null || locks.isEmpty()) {
            log.warn("尝试释放null或空锁列表");
            return;
        }

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();

        log.info("准备释放{}个锁 - 线程: {}, 线程ID: {}", locks.size(), threadName, threadId);

        int successCount = 0;
        int skipCount = 0;

        for (RLock lock : locks) {
            try {
                if (lock == null) {
                    continue;
                }

                // 检查当前线程是否持有该锁
                if (!lock.isHeldByCurrentThread()) {
                    log.debug("当前线程不持有该锁，跳过释放");
                    skipCount++;
                    continue;
                }

                lock.unlock();
                successCount++;
                log.debug("成功释放锁");

            } catch (Exception e) {
                log.error("释放锁异常 - 线程: {}, 异常: {}", threadName, e.getMessage(), e);
            }
        }

        log.info("锁释放完成 - 线程: {}, 线程ID: {}, 成功: {}, 跳过: {}, 总数: {}",
                threadName, threadId, successCount, skipCount, locks.size());
    }

}
