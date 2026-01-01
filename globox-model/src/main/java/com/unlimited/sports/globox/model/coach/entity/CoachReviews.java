package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * @since 2025/12/29 11:57
 * 教练课程评价表
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_reviews")
public class CoachReviews implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评价ID
     */
    @TableId(value = "coach_reviews_id", type = IdType.AUTO)
    private Long coachReviewsId;

    /**
     * 订单ID
     */
    @TableField(value = "coach_booking_id")
    private Long coachBookingId;

    /**
     * 评价用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 被评价教练ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 综合评分：1-5星
     */
    @TableField(value = "overall_rating")
    private Integer overallRating;

    /**
     * 专业度评分：1-5
     */
    @TableField(value = "professionalism_rating")
    private Integer professionalismRating;

    /**
     * 教学质量：1-5
     */
    @TableField(value = "teaching_rating")
    private Integer teachingRating;

    /**
     * 服务态度：1-5
     */
    @TableField(value = "attitude_rating")
    private Integer attitudeRating;

    /**
     * 评价内容
     */
    @TableField(value = "review_content")
    private String reviewContent;

    /**
     * 评价图片URL数组
     */
    @TableField(value = "review_images")
    private String reviewImages;

    /**
     * 评价标签：["耐心","专业","准时"]
     */
    @TableField(value = "review_tags")
    private String reviewTags;

    /**
     * 父评论ID（用于回复功能）
     */
    @TableField(value = "parent_review_id")
    private Long parentReviewId;

    /**
     * 评论类型：1-学员评价，2-教练回复
     */
    @TableField(value = "review_type")
    private Integer reviewType;


    /**
     * 是否匿名：0-否，1-是
     */
    @TableField(value = "is_anonymous")
    private Integer isAnonymous;

    /**
     * 状态：0-已隐藏，1-正常显示
     */
    @TableField(value = "review_status")
    private Integer reviewStatus;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}