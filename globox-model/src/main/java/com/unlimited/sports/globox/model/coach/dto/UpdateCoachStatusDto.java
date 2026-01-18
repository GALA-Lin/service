package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * @since 2026/1/12
 * 更新教练状态DTO
 */
@Data
public class UpdateCoachStatusDto {

    /**
     * 教练用户ID（从请求头获取）
     */
    private Long coachUserId;

    /**
     * 教练状态：0-暂停接单，1-正常接单，2-休假中
     */
    @NotNull(message = "教练状态不能为空")
    @Min(value = 0, message = "教练状态必须在0-2之间")
    @Max(value = 2, message = "教练状态必须在0-2之间")
    private Integer coachStatus;
}