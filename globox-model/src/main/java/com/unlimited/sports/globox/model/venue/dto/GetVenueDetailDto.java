package com.unlimited.sports.globox.model.venue.dto;


import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class GetVenueDetailDto {
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;
}
