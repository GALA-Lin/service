package com.unlimited.sports.globox.model.venue.dto;


import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 发布场馆评论请求DTO
 */
@Data
public class PostVenueReviewDto {


    private Long venueId;

    private Long userId;

    private Long parentReviewId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分不能小于1")
    @Max(value = 5, message = "评分不能大于5")
    private Integer rating;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    private List<String> imageUrls;

    private Boolean isAnonymous = false;
}
