package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

/**
 * @since 2026/1/12
 * 更新教练位置DTO
 */
@Data
public class UpdateCoachLocationDto {

    /**
     * 教练用户ID（从请求头获取）
     */
    private Long coachUserId;

    /**
     * 纬度
     */
    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度范围：-90 到 90")
    @DecimalMax(value = "90.0", message = "纬度范围：-90 到 90")
    private Double latitude;

    /**
     * 经度
     */
    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度范围：-180 到 180")
    @DecimalMax(value = "180.0", message = "经度范围：-180 到 180")
    private Double longitude;
}