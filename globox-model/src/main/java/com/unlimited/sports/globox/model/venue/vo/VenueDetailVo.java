package com.unlimited.sports.globox.model.venue.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class VenueDetailVo {
    @NonNull
    private Long venueId;

    @NonNull
    private String name;

    @NonNull
    private List<String> imageUrls;

    @NonNull
    private BigDecimal avgRating;

    @NonNull
    private Integer ratingCount;

    @NonNull
    private Integer courtCount;

    @NonNull
    private List<Integer> courtTypes;

    @NonNull
    private List<String> courtTypesDesc;


    @NonNull
    private String address;

    @NonNull
    private String phone;

    @NonNull
    private String description;

    @NonNull
    private List<String> facilities;

    @NonNull
    private String defaultOpenTime;

    @NonNull
    private String defaultCloseTime;

    @NonNull
    private BigDecimal minPrice;


    /**
     * 纬度
     */
    @NonNull
    private BigDecimal latitude;

    /**
     * 经度
     */
    @NonNull
    private BigDecimal longitude;
}
