package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2025/12/31 13:56
 * 教练评价Vo
 */

@Data
@Builder
public class CoachReviewVo {

    /**
     * 评价ID
     */
    @NonNull
    private Long reviewId;

    /**
     * 评价用户ID
     */
    @NonNull
    private Long userId;

    /**
     * 评价用户昵称
     */
    @NonNull
    private String userName;

    /**
     * 评价用户头像
     */
    @NonNull
    private String userAvatar;

    /**
     * 用户NTRP水平（1.0-7.0）
     */
    private BigDecimal userNtrpLevel;

    /**
     * 用户NTRP水平描述
     */
    private String ntrpLevelDesc;

    /**
     * 综合评分（1-5星）
     */
    @NonNull
    private Integer overallRating;

    /**
     * 专业度评分（1-5）
     */
    private Integer professionalismRating;

    /**
     * 教学质量评分（1-5）
     */
    private Integer teachingRating;

    /**
     * 服务态度评分（1-5）
     */
    private Integer attitudeRating;

    /**
     * 评价内容
     */
    @NonNull
    private String reviewContent;

    /**
     * 评价图片URL列表
     */
    private List<String> reviewImages;

    /**
     * 评价标签列表
     * 如：["耐心", "专业", "准时"]
     */
    private List<String> reviewTags;

    /**
     * 教练回复
     */
    private String coachReply;

    /**
     * 教练回复时间
     */
    private LocalDateTime coachReplyTime;

    /**
     * 是否匿名
     */
    @NonNull
    private Boolean isAnonymous;

    /**
     * 评价创建时间
     */
    @NonNull
    private LocalDateTime createdAt;
}
