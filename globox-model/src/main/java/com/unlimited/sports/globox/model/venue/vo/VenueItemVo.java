package com.unlimited.sports.globox.model.venue.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class VenueItemVo {

    @NonNull
    private Long venueId;

    @NonNull
    private String name;

    @NonNull
    private String region;

    private BigDecimal distance;

    @NonNull
    private String coverImage;

    @NonNull
    private BigDecimal avgRating;

    @NonNull
    private Integer ratingCount;

    @NonNull
    private BigDecimal minPrice;

    @NonNull
    private List<Integer> courtTypes;

    @NonNull
    private List<String> courtTypesDesc;

    @NonNull
    private List<Integer> groundTypes;

    @NonNull
    private List<String> groundTypesDesc;

    @NonNull
    private List<String> facilities;

    @NonNull
    private Integer courtCount;

    @NonNull
    private BigDecimal latitude;

    @NonNull
    private BigDecimal longitude;
}
