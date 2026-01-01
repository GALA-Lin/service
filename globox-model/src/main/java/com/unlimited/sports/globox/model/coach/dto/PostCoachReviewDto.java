package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 发布教练评价DTO
 */
@Data
public class PostCoachReviewDto {

    /**
     * 订单ID（必填）
     */
    @NotNull(message = "订单ID不能为空")
    private Long coachBookingId;

    /**
     * 教练用户ID（必填）
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

    /**
     * 用户ID（从请求头获取，不需要前端传）
     */
    private Long userId;

    /**
     * 综合评分：1-5星（必填）
     */
    @NotNull(message = "综合评分不能为空")
    @Min(value = 1, message = "评分必须在1-5之间")
    @Max(value = 5, message = "评分必须在1-5之间")
    private Integer overallRating;

    /**
     * 专业度评分：1-5（可选）
     */
    @Min(value = 1, message = "评分必须在1-5之间")
    @Max(value = 5, message = "评分必须在1-5之间")
    private Integer professionalismRating;

    /**
     * 教学质量评分：1-5（可选）
     */
    @Min(value = 1, message = "评分必须在1-5之间")
    @Max(value = 5, message = "评分必须在1-5之间")
    private Integer teachingRating;

    /**
     * 服务态度评分：1-5（可选）
     */
    @Min(value = 1, message = "评分必须在1-5之间")
    @Max(value = 5, message = "评分必须在1-5之间")
    private Integer attitudeRating;

    /**
     * 评价内容（必填）
     */
    @NotBlank(message = "评价内容不能为空")
    @Size(max = 1000, message = "评价内容不能超过1000字")
    private String reviewContent;

    /**
     * 评价图片URL列表（可选，最多9张）
     */
    @Size(max = 9, message = "最多上传9张图片")
    private List<String> reviewImages;

    /**
     * 评价标签列表（可选，最多10个）
     */
    @Size(max = 10, message = "最多选择10个标签")
    private List<String> reviewTags;

    /**
     * 是否匿名（默认false）
     */
    private Boolean isAnonymous = false;
}