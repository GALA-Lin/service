package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * d
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PricingActivityRequestDto implements Serializable {

    private Long userId;

    private Long activityId;

}
