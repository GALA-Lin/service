package com.unlimited.sports.globox.model.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *  用户创建活动订单 - 请求 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVenueActivityOrderDto {

    private Long activityId;

}
