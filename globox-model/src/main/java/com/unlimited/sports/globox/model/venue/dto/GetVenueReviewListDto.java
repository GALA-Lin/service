package com.unlimited.sports.globox.model.venue.dto;


import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 获取场馆一级评论列表请求DTO
 */
@Data
public class GetVenueReviewListDto {

    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;
}
