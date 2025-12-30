package com.unlimited.sports.globox.model.venue.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场馆评论VO
 */
@Data
@Builder
public class VenueReviewVo {

    @NonNull
    private Long reviewId;

    @NonNull
    private Long userId;

    @NonNull
    private String userName;

    @NonNull
    private String userAvatar;

    @NonNull
    private Integer rating;

    @NonNull
    private String content;

    @NonNull
    private List<String> imageUrls;

    @NonNull
    private LocalDateTime createdAt;

    private Integer replyCount;
}
