package com.unlimited.sports.globox.user.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.model.auth.dto.MerchantLoginRequest;
import com.unlimited.sports.globox.model.auth.dto.MerchantLoginResponse;
import com.unlimited.sports.globox.user.service.MerchantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家认证控制器
 */
@RestController
@RequestMapping("/auth/merchant")
@Tag(name = "商家认证", description = "商家登录相关接口")
public class MerchantAuthController {

    @Autowired
    private MerchantAuthService merchantAuthService;

    @PostMapping("/login")
    @Operation(summary = "商家账号密码登录", description = "商家使用账号和密码登录，不支持注册")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功"),
            @ApiResponse(responseCode = "2060", description = "账号不存在，请检查账号是否正确或联系管理员"),
            @ApiResponse(responseCode = "2061", description = "账号或密码错误，请重新输入"),
            @ApiResponse(responseCode = "2062", description = "账号已被禁用，请联系管理员")
    })
    public R<MerchantLoginResponse> merchantLogin(
            @Validated @RequestBody MerchantLoginRequest request) {
        MerchantLoginResponse response = merchantAuthService.merchantLogin(request);
        return R.ok(response);
    }
}
