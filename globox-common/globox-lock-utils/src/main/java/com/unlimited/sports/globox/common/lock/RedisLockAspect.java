package com.unlimited.sports.globox.common.lock;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RedisLockAspect {

    private final RedissonClient redissonClient;

    private final ExpressionParser spelParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    public RedisLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Pointcut("@annotation(com.unlimited.sports.globox.common.lock.RedisLock)")
    public void lockPointcut() {}

    @Around("lockPointcut() && @annotation(redisLock)")
    public Object around(ProceedingJoinPoint pjp, RedisLock redisLock) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        String lockKey = buildLockKey(redisLock, method, pjp.getArgs());
        RLock lock = redisLock.fair() ? redissonClient.getFairLock(lockKey) : redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            locked = tryAcquire(lock, redisLock);

            if (!locked) {
                String msg = "获取分布式锁失败: key=" + lockKey;
                if (redisLock.failFast()) {
                    throw new LockAcquireException(msg);
                }
                return null; // 不推荐：最好 failFast 抛异常由全局异常处理返回“处理中”
            }

            // 关键：事务存在时，事务完成后再释放锁（避免未提交就释放）
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                registerUnlockAfterTx(lock, lockKey);
                // 这里不要在 finally 里 unlock 了（交给 afterCompletion）
                return pjp.proceed();
            }

            // 无事务：正常执行，finally 里释放
            return pjp.proceed();

        } finally {
            // 仅在【无事务】或【未注册 afterCompletion】的情况下释放
            if (locked && !TransactionSynchronizationManager.isActualTransactionActive()) {
                safeUnlock(lock, lockKey);
            }
        }
    }

    private boolean tryAcquire(RLock lock, RedisLock cfg) throws InterruptedException {
        long waitTime = cfg.waitTime();
        long leaseTime = cfg.leaseTime();
        TimeUnit unit = cfg.waitTimeUnit();

        // leaseTime <= 0：不指定 leaseTime，走 watchdog 自动续期（推荐）
        if (leaseTime <= 0) {
            // tryLock(waitTime, unit) 存在则用；没有的话就 lock() + 自己控制超时策略
            return lock.tryLock(waitTime, unit);
        }

        return lock.tryLock(waitTime, leaseTime, unit);
    }

    private void registerUnlockAfterTx(RLock lock, String lockKey) {
        // 避免重复注册
        String resourceKey = "REDIS_LOCK::" + lockKey;
        if (TransactionSynchronizationManager.hasResource(resourceKey)) {
            return;
        }
        TransactionSynchronizationManager.bindResource(resourceKey, Boolean.TRUE);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                try {
                    safeUnlock(lock, lockKey);
                } finally {
                    // 清理标记
                    if (TransactionSynchronizationManager.hasResource(resourceKey)) {
                        TransactionSynchronizationManager.unbindResource(resourceKey);
                    }
                }
            }
        });
    }

    private void safeUnlock(RLock lock, String lockKey) {
        try {
            // 只释放自己线程持有的锁，避免误解锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: key={}", lockKey, e);
        }
    }

    private String buildLockKey(RedisLock cfg, Method method, Object[] args) {
        String spel = cfg.value();

        StandardEvaluationContext ctx = new StandardEvaluationContext();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        Expression exp = spelParser.parseExpression(spel);
        String dynamic = exp.getValue(ctx, String.class);

        String prefix = cfg.prefix();
        if (prefix != null && !prefix.isBlank()) {
            return prefix + dynamic;
        }
        return dynamic;
    }
}