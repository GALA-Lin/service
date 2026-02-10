package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @since 2026/2/9
 * 更新教练真名显示设置DTO
 */
@Data
public class UpdateCoachRealNameSettingsDto {

    /**
     * 教练用户ID（从请求头获取）
     */
    private Long coachUserId;

    /**
     * 教练真实姓名
     */
    @Size(max = 50, message = "真实姓名不能超过32个字符")
    private String coachRealName;

    /**
     * 是否显示真名：true-显示，false-不显示
     */
    @NotNull(message = "是否显示真名不能为空")
    private Boolean displayRealName;
}