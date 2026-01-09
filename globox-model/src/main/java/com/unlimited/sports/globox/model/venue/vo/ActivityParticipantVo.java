package com.unlimited.sports.globox.model.venue.vo;

import lombok.*;

import java.math.BigDecimal;

/**
 * 活动参与者信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityParticipantVo {

    /**
     * 用户ID
     */
    @NonNull
    private Long userId;

    /**
     * 用户头像
     */
    @NonNull
    private String avatarUrl;

    /**
     * 用户昵称
     */
    @NonNull
    private String nickName;

    /**
     * 用户NTRP等级
     */
    private Double userNtrpLevel;

    /**
     * 用户参与活动的次数
     */
    @NonNull
    @Builder.Default
    private Integer participationCount = 0;
}
