package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.model.venue.entity.venues.VenueThirdPartyConfig;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.dto.ThirdPartyAuthInfo;

/**
 * 第三方平台Token管理服务
 */
public interface IThirdPartyTokenService {

    /**
     * 获取认证信息（优先从缓存获取，缓存没有则登录获取）
     *
     * @param config 第三方平台配置
     * @param adapter 平台适配器实例
     * @return 认证信息（包含Token、adminId等）
     */
    ThirdPartyAuthInfo getAuthInfo(VenueThirdPartyConfig config, ThirdPartyPlatformAdapter adapter);

    /**
     * 刷新认证信息（重新登录并更新缓存）
     *
     * @param config 第三方平台配置
     * @param adapter 平台适配器实例
     * @return 新的认证信息
     */
    ThirdPartyAuthInfo refreshAuthInfo(VenueThirdPartyConfig config, ThirdPartyPlatformAdapter adapter);

    /**
     * 清除认证信息缓存
     *
     * @param config 第三方平台配置
     */
    void clearAuthInfo(VenueThirdPartyConfig config);
}
