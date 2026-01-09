package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2025/12/31 13:56
 * 教练/用户共用教练评价Vo
 */
@Data
@Builder
public class CoachReviewVo {

    /**
     * 评价ID
     */
    private Long reviewId;

    /**
     * 评价用户ID（匿名时返回-1）
     */
    private Long userId;

    /**
     * 评价用户昵称（匿名时显示"匿名用户"）
     */
    private String userName;

    /**
     * 评价用户头像
     */
    private String userAvatar;

    /**
     * 用户NTRP水平（1.0-7.0）
     */
    private Double userNtrpLevel;

    /**
     * 用户NTRP水平描述
     */
    private String ntrpLevelDesc;

    /**
     * 综合评分（1-5星）
     */
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
    private String reviewContent;

    /**
     * 评价图片URL列表
     */
    private List<String> reviewImages;

    /**
     * 评价标签列表
     */
    private List<String> reviewTags;

    /**
     * 是否匿名
     */
    private Boolean isAnonymous;

    /**
     * 回复数量
     */
    private Integer replyCount;

    /**
     * 评价创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 教练回复（可能为null）
     */
    private CoachReplyVo coachReply;

    /**
     * 教练回复VO
     */
    @Data
    @Builder
    public static class CoachReplyVo {
        /**
         * 回复ID
         */
        private Long replyId;

        /**
         * 教练用户ID
         */
        private Long coachUserId;

        /**
         * 教练昵称
         */
        private String coachName;

        /**
         * 教练头像
         */
        private String coachAvatar;

        /**
         * 回复内容
         */
        private String replyContent;

        /**
         * 回复时间
         */
        private LocalDateTime replyTime;
    }
}
