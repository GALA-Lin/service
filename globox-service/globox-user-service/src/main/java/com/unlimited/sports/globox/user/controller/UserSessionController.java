package com.unlimited.sports.globox.user.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Tag(name = "用户会话", description = "用户会话相关接口")
public class UserSessionController {

    @Autowired
    private AuthService authService;

    @PostMapping("/logout")
    @Operation(summary = "登出", description = "登出当前设备。\n" +
            "注意：仅对 app 端生效（其他端调用无副作用但会直接返回成功）。\n" +
            "需要携带 X-Client-Type=app 请求头，否则不会清理缓存。\n" +
            "登出后会立刻失效当前端的 access token 和 refresh token。")
    public R<Void> logout() {
        return authService.logout();
    }
}