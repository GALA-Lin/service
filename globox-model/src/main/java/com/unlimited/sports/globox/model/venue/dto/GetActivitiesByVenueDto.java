package com.unlimited.sports.globox.model.venue.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 根据场馆和日期查询活动列表DTO
 */
@Data
public class GetActivitiesByVenueDto {

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    /**
     * 活动日期
     */
    @NotNull(message = "活动日期不能为空")
    private LocalDate activityDate;
}
