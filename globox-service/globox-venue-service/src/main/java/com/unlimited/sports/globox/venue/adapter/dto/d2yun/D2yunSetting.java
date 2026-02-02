package com.unlimited.sports.globox.venue.adapter.dto.d2yun;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * D2yun场馆设置信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class D2yunSetting {

    @JsonProperty("stadium_id")
    private Long stadiumId;

    @JsonProperty("business_id")
    private Long businessId;

    @JsonProperty("stadium_name")
    private String stadiumName;

    @JsonProperty("stadium_address")
    private String stadiumAddress;

    /**
     * 时间槽长度（分钟）
     */
    private Integer duration;

    /**
     * 开始时间（分钟，从0点开始计算）
     */
    @JsonProperty("start_time")
    private Integer startTime;

    /**
     * 结束时间（分钟，从0点开始计算）
     */
    @JsonProperty("end_time")
    private Integer endTime;
}
