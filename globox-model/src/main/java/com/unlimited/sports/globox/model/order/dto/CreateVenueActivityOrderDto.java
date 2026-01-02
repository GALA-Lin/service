package com.unlimited.sports.globox.model.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 用户创建活动订单 - 请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateVenueActivityOrderDto", description = "用户创建活动订单请求参数")
public class CreateVenueActivityOrderDto {

    /**
     * 活动ID
     */
    @NotNull(message = "活动ID不能为空")
    @Schema(
            description = "活动ID",
            example = "10001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long activityId;
}
