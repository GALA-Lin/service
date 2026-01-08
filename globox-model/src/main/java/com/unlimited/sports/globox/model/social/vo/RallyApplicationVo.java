package com.unlimited.sports.globox.model.social.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 球帖申请信息VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RallyApplicationVo {
    /**
     * 申请ID
     */
    private Long id;
    /**
     * 集结帖ID
     */
    private Long rallyPostId;
    /**
     * 申请人ID
     */
    private Long applicantId;
    /**
     * 申请人头像URL
     */
    private String avatarUrl;
    /**
     * 申请人昵称
     */
    private String nickName;

    /**
     * 申请人网球水平（NTRP）
     */
    private BigDecimal userNtrpLevel;
    /**
     * 审核结果
     */
    private String inspectResult;
    /**
     * 申请时间
     */
    private LocalDateTime appliedAt;
    /**
     * 申请人用户名
     */
    private String applicantUsername;
    /**
     * 集结帖标题
     */
    private String rallyPostTitle;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
