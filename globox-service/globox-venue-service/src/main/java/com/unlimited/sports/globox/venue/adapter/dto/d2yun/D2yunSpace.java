package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * D2yun场地信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunSpace {

    @JsonProperty("space_id")
    private Long spaceId;

    @JsonProperty("business_id")
    private Long businessId;

    @JsonProperty("stadium_id")
    private Long stadiumId;

    @JsonProperty("space_name")
    private String spaceName;

    @JsonProperty("space_status")
    private Integer spaceStatus;

    @JsonProperty("online_status")
    private Integer onlineStatus;
}
