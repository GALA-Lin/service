package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @since 2026/1/1 16:41
 * 教练回复评论Dto
 */
@Data
public class PostCoachReplyDto {

    /**
     * 父评论ID（必填） 路径参数有
     */
    private Long parentReviewId;

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
     * 回复内容（必填）
     */
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容不能超过1000字")
    private String replyContent;
}
