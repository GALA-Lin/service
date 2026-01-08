package com.unlimited.sports.globox.venue.service.impl;

import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.service.RedisService;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyAuthInfo;
import com.unlimited.sports.globox.venue.service.IThirdPartyTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 第三方平台Token管理服务实现
 */
@Slf4j
@Service
public class ThirdPartyTokenServiceImpl implements IThirdPartyTokenService {

    @Autowired
    private RedisService redisService;

    private static final String AUTH_INFO_REDIS_KEY_PREFIX = "third_party:auth:";
    private static final long AUTH_INFO_TTL_HOURS = 23;


    @Override
    public ThirdPartyAuthInfo getAuthInfo(VenueThirdPartyConfig config, ThirdPartyPlatformAdapter adapter) {
        String redisKey = buildAuthInfoKey(config);

        // 先从Redis获取
        ThirdPartyAuthInfo authInfo = redisService.getCacheObject(redisKey, ThirdPartyAuthInfo.class);

        if (authInfo != null) {
            log.debug("[认证管理] 从缓存获取认证信息成功: venueId={}, platformId={}",
                    config.getVenueId(), config.getThirdPartyPlatformId());
            return authInfo;
        }

        // 缓存没有，重新登录获取
        log.info("[认证管理] 缓存中无认证信息，开始登录: venueId={}, platformId={}",
                config.getVenueId(), config.getThirdPartyPlatformId());
        return refreshAuthInfo(config, adapter);
    }

    @Override
    public ThirdPartyAuthInfo refreshAuthInfo(VenueThirdPartyConfig config, ThirdPartyPlatformAdapter adapter) {
        // 直接调用传入的adapter的登录接口
        String platformCode = adapter.getPlatformCode();
        log.info("[认证管理] 开始登录获取认证信息: venueId={}, platformCode={}",
                config.getVenueId(), platformCode);

        ThirdPartyAuthInfo authInfo = adapter.login(config);

        if (authInfo == null || authInfo.getToken() == null) {
            log.error("[认证管理] 登录失败，无法获取认证信息: venueId={}, platformCode={}",
                    config.getVenueId(), platformCode);
            throw new RuntimeException("登录第三方平台失败，无法获取认证信息");
        }

        // 存入Redis缓存（TTL=23小时）
        String redisKey = buildAuthInfoKey(config);
        redisService.setCacheObject(redisKey, authInfo, AUTH_INFO_TTL_HOURS, TimeUnit.HOURS);

        log.info("[认证管理] 认证信息刷新成功并已缓存: venueId={}, platformCode={}, ttl={}h",
                config.getVenueId(), platformCode, AUTH_INFO_TTL_HOURS);

        return authInfo;
    }

    @Override
    public void clearAuthInfo(VenueThirdPartyConfig config) {
        String redisKey = buildAuthInfoKey(config);
        redisService.deleteObject(redisKey);
        log.info("[认证管理] 认证信息缓存已清除: venueId={}, platformId={}",
                config.getVenueId(), config.getThirdPartyPlatformId());
    }

    /**
     * 构建认证信息的Redis Key
     */
    private String buildAuthInfoKey(VenueThirdPartyConfig config) {
        return AUTH_INFO_REDIS_KEY_PREFIX + config.getThirdPartyPlatformId() + ":" + config.getVenueId();
    }
}
