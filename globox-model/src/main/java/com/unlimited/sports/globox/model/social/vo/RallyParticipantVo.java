package com.unlimited.sports.globox.model.social.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 参与者VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RallyParticipantVo {

    /**
     * 参与者id
     */
    private Long participantId;

    /**
     * 参与者URL
     */
    private String avatarUrl;

    /**
     * 参与者昵称
     */
    private String nickName;

    /**
     * 参与者的网球等级
     */
    private double userNtrp;
    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;

    /**
     * 是否是发起人
     */
    private boolean isInitiator;
}
