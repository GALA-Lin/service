package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;

/**
 * 第三方平台Token管理服务
 */
public interface IThirdPartyTokenService {

    /**
     * 获取Token（优先从缓存获取，缓存没有则登录获取）
     *
     * @param config 第三方平台配置
     * @return Token
     */
    String getToken(VenueThirdPartyConfig config);

    /**
     * 刷新Token（重新登录并更新缓存）
     *
     * @param config 第三方平台配置
     * @return 新Token
     */
    String refreshToken(VenueThirdPartyConfig config);

    /**
     * 清除Token缓存
     *
     * @param config 第三方平台配置
     */
    void clearToken(VenueThirdPartyConfig config);
}
