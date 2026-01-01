package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * 创建订单前查询价格 - 结果 DTO
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PricingActivityResultDto extends PricingResultDto implements Serializable {

    private Long activityId;

    private String activityTypeCode;

    private String activityTypeName;
}


