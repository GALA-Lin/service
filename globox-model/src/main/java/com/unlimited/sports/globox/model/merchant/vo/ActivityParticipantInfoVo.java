package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 活动参与者信息VO
 * 用于商家端查看活动报名用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityParticipantInfoVo {

    /**
     * 用户ID
     */
    @NonNull
    private Long userId;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 用户昵称
     */
    private String nickName;
}
