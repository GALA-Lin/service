package com.unlimited.sports.globox.model.venue.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 删除场馆评论请求DTO
 */
@Data
public class DeleteVenueReviewDto {

    @NotNull(message = "评论ID不能为空")
    private Long reviewId;

    private Long userId;

    private Integer deleteOperatorType;
}
