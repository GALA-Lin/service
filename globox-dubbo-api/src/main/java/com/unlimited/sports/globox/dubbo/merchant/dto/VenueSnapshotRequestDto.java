package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 订单服务获取 venue 信息 - request dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueSnapshotRequestDto implements Serializable {

    @NotNull
    private Long userId;

    @NotNull
    private Long venueId;

    @NotNull
    private List<Long> courtId;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;
}
