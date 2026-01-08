package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 微信登录响应
 *
 * @author Wreckloud
 * @since 2025/12/20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信登录响应")
public class WechatLoginResponse {

    @Schema(description = "Access Token（格式：Bearer xxx，已绑定时返回）", example = "Bearer eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Refresh Token（已绑定时返回）", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "用户ID（已绑定时返回）", example = "1")
    private Long userId;

    @Schema(description = "用户角色列表（已绑定时返回）", example = "[\"USER\"]")
    private List<String> roles;

    @Schema(description = "是否为新用户（已绑定时返回）", example = "true")
    private Boolean isNewUser;

    @Schema(description = "用户信息（已绑定时返回）")
    private ThirdPartyLoginResponse.UserInfo userInfo;

    @Schema(description = "是否需要绑定手机号", example = "true")
    private Boolean needBindPhone;

    @Schema(description = "临时凭证（未绑定时返回，用于后续绑定手机号）", example = "temp_abc123def456")
    private String tempToken;

    @Schema(description = "微信昵称（可选，未绑定时返回）", example = "微信用户")
    private String nickname;

    @Schema(description = "微信头像（可选，未绑定时返回）", example = "https://...")
    private String avatar;
}

