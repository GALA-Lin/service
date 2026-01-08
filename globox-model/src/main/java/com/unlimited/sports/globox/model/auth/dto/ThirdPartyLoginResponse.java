package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 第三方小程序登录响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "第三方小程序登录响应")
public class ThirdPartyLoginResponse {

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Schema(description = "是否为新用户", example = "true")
    private Boolean isNewUser;

    /**
     * 用户信息内嵌类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID", example = "1")
        private Long id;

        @Schema(description = "手机号", example = "13800138000")
        private String phone;

        @Schema(description = "昵称", example = "用户1234")
        private String nickname;

        @Schema(description = "头像URL", example = "https://...")
        private String avatarUrl;
    }
}

