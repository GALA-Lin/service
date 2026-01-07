package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.model.auth.dto.MerchantLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.MerchantLoginResponse;

/**
 * 商家认证服务接口
 */
public interface MerchantAuthService {

    /**
     * 商家账号密码登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    MerchantLoginResponse merchantLogin(MerchantLoginRequest request);
}
