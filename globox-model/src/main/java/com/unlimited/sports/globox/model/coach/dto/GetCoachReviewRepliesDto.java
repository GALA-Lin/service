package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @since 2026/1/1 12:26
 * 获取评价回复列表DTO
 */
@Data
public class GetCoachReviewRepliesDto {

    /**
     * 父评论ID
     */
    @NotNull(message = "评论ID不能为空")
    private Long parentReviewId;

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 50, message = "每页大小不能超过50")
    private Integer pageSize = 10;
}
