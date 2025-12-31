package com.unlimited.sports.globox.venue.lock;

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
     * 使用Redisson MultiLock批量获取多个锁
     * 内部自动排序避免死锁
     *
     * @param lockKeys 锁键列表
     * @param waitTime 等待时间
     * @param leaseTime 锁持有时间
     * @param timeUnit 时间单位
     * @return 所有锁都获取成功返回MultiLock对象，否则返回null
     */
    public RLock tryLockMultiple(List<String> lockKeys, long waitTime, long leaseTime, TimeUnit timeUnit) {
        if (lockKeys == null || lockKeys.isEmpty()) {
            log.warn("锁键列表为空");
            return null;
        }

        // 去重并排序，避免死锁
        List<String> sortedKeys = lockKeys.stream()
                .distinct()
                .sorted()
                .toList();

        log.debug("开始使用MultiLock批量获取锁，共{}个锁，已排序", sortedKeys.size());

        try {
            // 创建所有RLock对象
            List<RLock> locks = sortedKeys.stream()
                    .map(redissonClient::getLock)
                    .toList();

            // 使用Redisson MultiLock批量获取所有锁
            // MultiLock内部会自动排序和处理失败回滚，比循环加锁更高效
            RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

            // 方法签名: tryLock(waitTime, leaseTime, unit)
            // 指定leaseTime确保锁在业务逻辑执行期间保持有效
            boolean acquired = multiLock.tryLock(waitTime, leaseTime, timeUnit);

            if (acquired) {
                log.info("成功获取所有锁（MultiLock），共{}个，等待时间: {} {}，锁持有时间: {} {}",
                        locks.size(), waitTime, timeUnit, leaseTime, timeUnit);
                return multiLock;
            } else {
                log.warn("获取锁失败，在 {} {} 内未能成功获取所有锁", waitTime, timeUnit);
                return null;
            }

        } catch (InterruptedException e) {
            log.error("获取锁被中断", e);
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.error("获取锁异常", e);
            return null;
        }
    }

    /**
     * 释放MultiLock锁
     * 直接释放MultiLock对象
     *
     * @param multiLock MultiLock对象
     */
    public void unlockMultiple(RLock multiLock) {
        if (multiLock == null) {
            return;
        }

        try {
            // 检查当前线程是否持有该锁
            if (!multiLock.isHeldByCurrentThread()) {
                log.warn("当前线程不持有该锁，跳过释放");
                return;
            }

            multiLock.unlock();
            log.info("MultiLock释放完成");

        } catch (Exception e) {
            log.error("释放MultiLock异常", e);
        }
    }

}
