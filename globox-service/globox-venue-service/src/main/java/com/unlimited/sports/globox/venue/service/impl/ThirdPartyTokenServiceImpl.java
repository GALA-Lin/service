package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.model.venue.entity.venues.ThirdPartyPlatform;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.service.RedisService;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapterFactory;
import com.unlimited.sports.globox.venue.mapper.ThirdPartyPlatformMapper;
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

    @Autowired
    private ThirdPartyPlatformAdapterFactory adapterFactory;

    @Autowired
    private ThirdPartyPlatformMapper platformMapper;

    private static final String TOKEN_REDIS_KEY_PREFIX = "third_party:token:";
    private static final long TOKEN_TTL_HOURS = 23;

    @Override
    public String getToken(VenueThirdPartyConfig config) {
        String redisKey = buildTokenKey(config);

        // 1. 先从Redis获取
        String token = redisService.getCacheObject(redisKey, String.class);

        if (token != null) {
            log.debug("[Token管理] 从缓存获取Token成功: venueId={}, platformId={}",
                    config.getVenueId(), config.getThirdPartyPlatformId());
            return token;
        }

        // 2. 缓存没有，重新登录获取
        log.info("[Token管理] 缓存中无Token，开始登录: venueId={}, platformId={}",
                config.getVenueId(), config.getThirdPartyPlatformId());
        return refreshToken(config);
    }

    @Override
    public String refreshToken(VenueThirdPartyConfig config) {
        // 1. 获取平台代码
        String platformCode = getPlatformCode(config.getThirdPartyPlatformId());

        // 2. 获取对应的适配器
        ThirdPartyPlatformAdapter adapter = adapterFactory.getAdapter(platformCode);

        // 3. 调用登录接口
        log.info("[Token管理] 开始登录获取Token: venueId={}, platformCode={}",
                config.getVenueId(), platformCode);
        String newToken = adapter.login(config);

        if (newToken == null) {
            log.error("[Token管理] 登录失败，无法获取Token: venueId={}, platformCode={}",
                    config.getVenueId(), platformCode);
            throw new RuntimeException("登录第三方平台失败，无法获取Token");
        }

        // 4. 存入Redis缓存（TTL=23小时）
        String redisKey = buildTokenKey(config);
        redisService.setCacheObject(redisKey, newToken, TOKEN_TTL_HOURS, TimeUnit.HOURS);

        log.info("[Token管理] Token刷新成功并已缓存: venueId={}, platformCode={}, ttl={}h",
                config.getVenueId(), platformCode, TOKEN_TTL_HOURS);

        return newToken;
    }

    @Override
    public void clearToken(VenueThirdPartyConfig config) {
        String redisKey = buildTokenKey(config);
        redisService.deleteObject(redisKey);
        log.info("[Token管理] Token缓存已清除: venueId={}, platformId={}",
                config.getVenueId(), config.getThirdPartyPlatformId());
    }

    /**
     * 构建Token的Redis Key
     */
    private String buildTokenKey(VenueThirdPartyConfig config) {
        return TOKEN_REDIS_KEY_PREFIX + config.getThirdPartyPlatformId() + ":" + config.getVenueId();
    }

    /**
     * 根据平台ID获取平台代码
     */
    private String getPlatformCode(Long platformId) {
        ThirdPartyPlatform platform = platformMapper.selectOne(
                new LambdaQueryWrapper<ThirdPartyPlatform>()
                        .eq(ThirdPartyPlatform::getPlatformId, platformId)
        );
        if (platform == null) {
            throw new IllegalArgumentException("第三方平台不存在: platformId=" + platformId);
        }
        return platform.getPlatformCode();
    }
}
