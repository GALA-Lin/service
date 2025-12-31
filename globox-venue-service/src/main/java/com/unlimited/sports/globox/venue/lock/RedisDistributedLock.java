package com.unlimited.sports.globox.venue.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis分布式锁工具类
 * 支持多资源顺序加锁，避免死锁
 */
@Slf4j
@Component
public class RedisDistributedLock {

    @Autowired
    private RedissonClient redissonClient;

    /**

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

        List<String> sortedKeys = lockKeys.stream()
                .distinct()  // 去重
                .sorted()    // 排序（字典序）
                .toList();

        log.debug("开始批量获取锁，共{}个锁，已排序", sortedKeys.size());

        List<RLock> acquiredLocks = new ArrayList<>();

        try {
            for (String lockKey : sortedKeys) {
                RLock lock = redissonClient.getLock(lockKey);
                boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

                if (acquired) {
                    acquiredLocks.add(lock);
                    log.debug("成功获取锁: {}", lockKey);
                } else {
                    log.error("获取锁失败: {}，开始回滚", lockKey);
                    // 获取锁失败，释放已获取的所有锁
                    unlockMultiple(acquiredLocks);
                    return null;
                }
            }

            log.info("成功获取所有锁，共{}个", acquiredLocks.size());
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
     * 批量释放锁
     *
     * @param locks 锁列表
     */
    public void unlockMultiple(List<RLock> locks) {
        if (locks == null || locks.isEmpty()) {
            return;
        }

        log.debug("开始释放{}个锁", locks.size());

        for (RLock lock : locks) {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                    log.debug("成功释放锁");
                }
            } catch (Exception e) {
                log.error("释放锁异常", e);
            }
        }

        log.info("批量释放锁完成，共{}个", locks.size());
    }

    /**
     * 获取单个锁
     *
     * @param lockKey 锁键
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param timeUnit 时间单位
     * @return 锁对象，获取失败返回null
     */
    public RLock tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        try {
            RLock lock = redissonClient.getLock(lockKey);
            boolean acquired = lock.tryLock(waitTime, leaseTime, timeUnit);

            if (acquired) {
                log.debug("成功获取单个锁: {}", lockKey);
                return lock;
            } else {
                log.warn("获取单个锁失败: {}", lockKey);
                return null;
            }
        } catch (InterruptedException e) {
            log.error("获取锁被中断: {}", lockKey, e);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.error("获取锁异常: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放单个锁
     *
     * @param lock 锁对象
     */
    public void unlock(RLock lock) {
        if (lock == null) {
            return;
        }

        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("成功释放单个锁");
            }
        } catch (Exception e) {
            log.error("释放锁异常", e);
        }
    }
}
