package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("venue_review")
@Builder
public class VenueReview {
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    private Long venueId;

    private Long userId;

    private Long parentReviewId;

    private Integer reviewType;

    private Integer rating;

    private String content;

    private String imageUrls;

    private Boolean isAnonymous;

    private Boolean deleted;

    private LocalDateTime deletedAt;

    private Integer deleteOperatorType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
