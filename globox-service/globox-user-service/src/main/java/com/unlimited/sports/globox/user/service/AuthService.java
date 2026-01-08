package com.unlimited.sports.globox.user.service;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.dto.LoginResponse;
import com.unlimited.sports.globox.model.auth.dto.PasswordLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.PhoneLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.ChangePasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.ResetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.SendCaptchaRequest;
import com.unlimited.sports.globox.model.auth.dto.SetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.TokenRefreshRequest;
import com.unlimited.sports.globox.model.auth.dto.ThirdPartyLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.WechatBindPhoneRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.WechatPhoneLoginRequest;

/**
 * 认证服务接口
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
public interface AuthService {

    /**
     * 发送短信验证码
     *
     * @param request 发送请求（手机号）
     * @return 成功返回提示信息
     */
    R<String> sendCaptcha(SendCaptchaRequest request);

    /**
     * 手机号+验证码登录（登录即注册）
     *
     * @param request 登录请求（手机号、验证码）
     * @return 登录响应（Token、用户信息）
     */
    R<LoginResponse> phoneLogin(PhoneLoginRequest request);

    /**
     * 微信授权登录
     *
     * @param request 微信登录请求（code）
     * @return 登录响应（已绑定返回Token，未绑定返回临时凭证）
     */
    R<WechatLoginResponse> wechatLogin(WechatLoginRequest request);

    /**
     * 微信绑定手机号
     *
     * @param request 绑定请求（临时凭证、手机号、验证码、微信信息）
     * @return 登录响应（Token、用户信息）
     */
    R<LoginResponse> wechatBindPhone(WechatBindPhoneRequest request);

    /**
     * 第三方小程序微信手机号登录
     *
     * @param request 登录请求（wxCode、phoneCode、nickname、avatarUrl）
     * @return 登录响应（Token、用户信息、是否为新用户）
     */
    R<ThirdPartyLoginResponse> wechatPhoneLogin(WechatPhoneLoginRequest request);

    /**
     * 设置密码
     *
     * @param request 设置密码请求
     * @return 成功响应
     */
    R<String> setPassword(SetPasswordRequest request);

    /**
     * 手机号密码登录
     *
     * @param request 登录请求（手机号、密码）
     * @return 登录响应（Token、用户信息）
     */
    R<LoginResponse> passwordLogin(PasswordLoginRequest request);

    /**
     * 找回密码
     *
     * @param request 找回密码请求（手机号、验证码、新密码）
     * @return 成功响应
     */
    R<String> resetPassword(ResetPasswordRequest request);

    /**
     * 修改密码
     *
     * @param request 修改密码请求（旧密码、新密码、确认密码）
     * @return 成功响应
     */
    R<String> changePassword(ChangePasswordRequest request);

    /**
     * 刷新 Token
     *
     * @param request 刷新请求（refreshToken）
     * @return 登录响应（新的 Token、用户信息）
     */
    R<LoginResponse> refreshToken(TokenRefreshRequest request);
}

