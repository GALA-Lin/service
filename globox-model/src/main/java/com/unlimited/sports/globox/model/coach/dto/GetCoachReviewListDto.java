package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * @since 2026/1/1 12:23
 * 获取教练评价列表Dto
 */
@Data
public class GetCoachReviewListDto {

    /**
     * 教练用户ID
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

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
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;

    /**
     * 评分筛选：1-5星，null表示不筛选
     */
    @Min(value = 1, message = "评分必须在1-5之间")
    @Max(value = 5, message = "评分必须在1-5之间")
    private Integer ratingFilter;

    /**
     * 是否只看有图评价：true-只看有图，false或null-不筛选
     */
    private Boolean withImagesOnly;
}

