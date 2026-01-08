package com.unlimited.sports.globox.user.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.dto.LoginResponse;
import com.unlimited.sports.globox.model.auth.dto.ChangePasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.PasswordLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.PhoneLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.ResetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.SendCaptchaRequest;
import com.unlimited.sports.globox.model.auth.dto.SetPasswordRequest;
import com.unlimited.sports.globox.model.auth.dto.TokenRefreshRequest;
import com.unlimited.sports.globox.model.auth.dto.ThirdPartyLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.WechatBindPhoneRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.WechatLoginResponse;
import com.unlimited.sports.globox.model.auth.dto.WechatPhoneLoginRequest;
import com.unlimited.sports.globox.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "认证模块", description = "用户登录、注册、验证码相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/captcha/send")
    @Operation(summary = "发送短信验证码", description = "发送登录/注册验证码，需要白名单权限")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "2001", description = "手机号格式不正确"),
            @ApiResponse(responseCode = "2002", description = "当前仅限内测用户，敬请期待正式上线"),
            @ApiResponse(responseCode = "2003", description = "验证码发送过于频繁，请稍后再试")
    })
    public R<String> sendCaptcha(@Validated @RequestBody SendCaptchaRequest request) {
        return authService.sendCaptcha(request);
    }

    @PostMapping("/phone/login")
    @Operation(summary = "手机号+验证码登录", description = "验证码登录，不存在则自动注册（登录即注册）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "2001", description = "手机号格式不正确"),
            @ApiResponse(responseCode = "2002", description = "当前仅限内测用户，敬请期待正式上线"),
            @ApiResponse(responseCode = "2004", description = "验证码错误或已过期"),
            @ApiResponse(responseCode = "2005", description = "验证码错误次数过多，请重新获取")
    })
    public R<LoginResponse> phoneLogin(@Validated @RequestBody PhoneLoginRequest request) {
        return authService.phoneLogin(request);
    }

    @PostMapping("/wechat/login")
    @Operation(summary = "微信授权登录", description = "微信授权登录，已绑定账号直接登录，未绑定返回临时凭证。小程序端必须携带 X-Client-Type: third-party-jsapi，否则不会生成包含 openid 的 token。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "2033", description = "微信授权失败"),
            @ApiResponse(responseCode = "2034", description = "微信授权code已过期")
    })
    public R<WechatLoginResponse> wechatLogin(@Validated @RequestBody WechatLoginRequest request) {
        return authService.wechatLogin(request);
    }

    @PostMapping("/wechat/bindPhone")
    @Operation(summary = "微信绑定手机号", description = "微信登录时绑定手机号，支持绑定到现有账号或创建新账号")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "绑定成功"),
            @ApiResponse(responseCode = "2001", description = "手机号格式不正确"),
            @ApiResponse(responseCode = "2002", description = "当前仅限内测用户，敬请期待正式上线"),
            @ApiResponse(responseCode = "2033", description = "微信授权失败"),
            @ApiResponse(responseCode = "2035", description = "临时凭证已过期，请重新授权")
    })
    public R<LoginResponse> wechatBindPhone(@Validated @RequestBody WechatBindPhoneRequest request) {
        return authService.wechatBindPhone(request);
    }

    @PostMapping("/wechat/phoneLogin")
    @Operation(summary = "第三方小程序微信手机号登录", description = "第三方小程序端微信手机号登录，通过wxCode和phoneCode一次性完成登录和手机号绑定，无需验证码。小程序端必须携带 X-Client-Type: third-party-jsapi，否则不会生成包含 openid 的 token。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "2001", description = "手机号格式不正确"),
            @ApiResponse(responseCode = "2002", description = "当前仅限内测用户，敬请期待正式上线"),
            @ApiResponse(responseCode = "2033", description = "微信授权失败"),
            @ApiResponse(responseCode = "2034", description = "微信授权code已过期")
    })
    public R<ThirdPartyLoginResponse> wechatPhoneLogin(@Validated @RequestBody WechatPhoneLoginRequest request) {
        return authService.wechatPhoneLogin(request);
    }

    @PostMapping("/password/set")
    @Operation(summary = "设置密码", description = "用户设置登录密码（首次或修改），需要登录后才能调用")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2013", description = "密码必须为6-20位"),
            @ApiResponse(responseCode = "2014", description = "两次输入的密码不一致"),
            @ApiResponse(responseCode = "2016", description = "已设置过密码，请使用修改密码")
    })
    public R<String> setPassword(@Validated @RequestBody SetPasswordRequest request) {
        return authService.setPassword(request);
    }

    @PostMapping("/password/login")
    @Operation(summary = "手机号密码登录", description = "使用手机号+密码登录，用户必须已设置密码")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "2010", description = "用户不存在，请使用验证码登录"),
            @ApiResponse(responseCode = "2011", description = "密码错误"),
            @ApiResponse(responseCode = "2012", description = "未设置密码，请使用验证码登录"),
            @ApiResponse(responseCode = "2015", description = "手机号或密码错误")
    })
    public R<LoginResponse> passwordLogin(@Validated @RequestBody PasswordLoginRequest request) {
        return authService.passwordLogin(request);
    }

    @PostMapping("/password/reset")
    @Operation(summary = "找回密码", description = "通过验证码重置密码，重置后清除所有Refresh Token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "重置成功"),
            @ApiResponse(responseCode = "2001", description = "手机号格式不正确"),
            @ApiResponse(responseCode = "2004", description = "验证码错误或已过期"),
            @ApiResponse(responseCode = "2005", description = "验证码错误次数过多，请重新获取")
    })
    public R<String> resetPassword(@Validated @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @PostMapping("/password/change")
    @Operation(summary = "修改密码", description = "已登录用户修改密码，需要验证旧密码，修改后清除所有Refresh Token")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "2021", description = "无效的Token"),
            @ApiResponse(responseCode = "2011", description = "密码错误"),
            @ApiResponse(responseCode = "2013", description = "密码必须为6-20位"),
            @ApiResponse(responseCode = "2014", description = "两次输入的密码不一致")
    })
    public R<String> changePassword(@Validated @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "刷新 Token", description = "使用 Refresh Token 刷新 Access Token 和 Refresh Token。refreshToken 旋转机制：旧 refreshToken 立即失效，返回新的 token 对。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "刷新成功"),
            @ApiResponse(responseCode = "2022", description = "Refresh Token无效或已过期")
    })
    public R<LoginResponse> refreshToken(@Validated @RequestBody TokenRefreshRequest request) {
        return authService.refreshToken(request);
    }
}

