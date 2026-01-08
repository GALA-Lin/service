package com.unlimited.sports.globox.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "Access Token（格式：Bearer xxx）", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Refresh Token", example = "550e8400-e29b-41d4-a716-446655440000:abc123")
    private String refreshToken;

    @Schema(description = "用户角色列表", example = "[\"player\"]")
    private List<String> roles;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "是否为新用户（首次登录）", example = "true")
    private Boolean isNewUser;
}
