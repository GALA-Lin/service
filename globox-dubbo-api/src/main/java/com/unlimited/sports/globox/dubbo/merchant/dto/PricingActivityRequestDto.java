package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * d
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingActivityRequestDto {

    private Long userId;

    private Long activityId;

}
