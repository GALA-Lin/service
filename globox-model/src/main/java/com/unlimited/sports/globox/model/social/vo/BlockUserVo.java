package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 拉黑用户列表项
 */
@Data
@Schema(description = "拉黑用户列表项")
public class BlockUserVo {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "头像")
    private String avatarUrl;

    @Schema(description = "拉黑时间（列表排序）")
    private LocalDateTime blockedAt;
}
