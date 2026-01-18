package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @since 2026/1/12
 * 更新教练场地偏好DTO
 */
@Data
public class UpdateCoachVenuePreferenceDto {

    /**
     * 教练用户ID（从请求头获取）
     */
    private Long coachUserId;

    /**
     * 接受场地类型：0-都可以，1-红土，2-草地，3-硬地
     */
    @NotNull(message = "场地类型不能为空")
    @Min(value = 0, message = "场地类型必须在0-3之间")
    @Max(value = 3, message = "场地类型必须在0-3之间")
    private Integer coachAcceptVenueType;
}