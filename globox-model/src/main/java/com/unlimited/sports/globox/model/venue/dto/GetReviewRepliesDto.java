package com.unlimited.sports.globox.model.venue.dto;


import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 获取评论回复列表请求DTO
 * 用于获取某个评论的子评论
 */
@Data
public class GetReviewRepliesDto {

    @NotNull(message = "父评论ID不能为空")
    private Long parentReviewId;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;
}
