package com.unlimited.sports.globox.model.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户搜索结果单项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户搜索结果单项")
public class UserSearchItemVo {

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "球盒号（保留用户输入大小写）", example = "GloboxPlayer123")
    private String username;

    @Schema(description = "昵称", example = "Ace Player")
    private String nickName;

    @Schema(description = "头像URL", example = "https://cdn.example.com/avatar.png")
    private String avatarUrl;
}


