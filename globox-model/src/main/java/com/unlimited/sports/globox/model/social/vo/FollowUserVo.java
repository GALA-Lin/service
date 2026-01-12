package com.unlimited.sports.globox.model.social.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注/粉丝用户列表项
 */
@Data
@Schema(description = "关注/粉丝用户列表项")
public class FollowUserVo {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "头像")
    private String avatarUrl;

    @Schema(description = "我是否关注TA")
    private Boolean isFollowed;

    @Schema(description = "是否互相关注")
    private Boolean isMutual;

    @Schema(description = "关注时间（列表排序）")
    private LocalDateTime followedAt;
}




