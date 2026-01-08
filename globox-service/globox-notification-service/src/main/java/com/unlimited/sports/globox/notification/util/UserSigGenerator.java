package com.unlimited.sports.globox.notification.util;

import com.tencentyun.TLSSigAPIv2;
import com.unlimited.sports.globox.notification.config.TencentCloudImProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯云IM管理员UserSig生成工具
 * 使用Redis缓存，避免频繁生成签名
 */
@Slf4j
@Component
public class UserSigGenerator {

    @Autowired
    private  TencentCloudImProperties properties;

    @Autowired
    private  RedisTemplate<String, String> redisTemplate;

    private TLSSigAPIv2 sigApi;

    // Redis缓存的Key
    private static final String ADMIN_USER_SIG_CACHE_KEY = "notification_admin_user_sig";

    // 缓存过期提前时间（1天，单位秒），用于在UserSig真正过期前重新生成
    private static final long CACHE_ADVANCE_EXPIRE_TIME = 86400L;


    /**
     * 初始化UserSig生成器
     */
    @PostConstruct
    public void init() {
        Long sdkAppId = properties.getSdkAppId();
        String secretKey = properties.getSecretKey();

        if (sdkAppId == null || secretKey == null) {
            throw new RuntimeException("腾讯云IM配置不完整：sdkAppId 或 secretKey 未配置");
        }

        try {
            this.sigApi = new TLSSigAPIv2(sdkAppId, secretKey);
            log.info("UserSigGenerator初始化成功: sdkAppId={}", sdkAppId);
        } catch (Exception e) {
            log.error("UserSigGenerator初始化失败", e);
            throw new RuntimeException("UserSigGenerator初始化失败", e);
        }
    }

    /**
     * 生成管理员UserSig（支持Redis缓存）
     *
     * @return UserSig签名
     */
    public String generateAdminSig() {
        // 尝试从Redis缓存获取
        String cachedSig = redisTemplate.opsForValue().get(ADMIN_USER_SIG_CACHE_KEY);
        if (cachedSig != null) {
            log.debug("[UserSig缓存命中] admin账号");
            return cachedSig;
        }

        log.debug("[UserSig生成] 开始生成管理员签名");

        // 生成新的UserSig
        String adminAccount = properties.getAdminAccount();
        String userSig;
        try {
            userSig = sigApi.genUserSig(adminAccount, properties.getUserSigExpire());
        } catch (Exception e) {
            log.error("[UserSig生成失败] admin账号={}", adminAccount, e);
            throw new RuntimeException("生成管理员UserSig失败", e);
        }

        // 将UserSig存入Redis缓存
        // 缓存过期时间 = UserSig有效期 - 提前过期时间
        long cacheExpireTime = properties.getUserSigExpire() - CACHE_ADVANCE_EXPIRE_TIME;
        redisTemplate.opsForValue().set(ADMIN_USER_SIG_CACHE_KEY, userSig, cacheExpireTime, TimeUnit.SECONDS);
        log.debug("[UserSig已缓存] cacheExpireTime={}秒", cacheExpireTime);

        return userSig;
    }
}
