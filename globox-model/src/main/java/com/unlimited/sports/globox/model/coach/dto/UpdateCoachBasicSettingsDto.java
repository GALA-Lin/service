package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @since 2026/1/12
 * 更新教练基本设置DTO
 */
@Data
public class UpdateCoachBasicSettingsDto {

    /**
     * 教练用户ID（从请求头获取）
     */
    private Long coachUserId;

    /**
     * 教学风格简述
     */
    @Size(max = 500, message = "教学风格不能超过500字")
    private String coachTeachingStyle;

    /**
     * 专长标签
     */
    @Size(max = 20, message = "专长标签最多20个")
    private List<String> coachSpecialtyTags;

    /**
     * 主要奖项
     */
    @Size(max = 10, message = "主要奖项最多10个")
    private List<String> coachAward;

    /**
     * 教龄
     */
    @Min(value = 0, message = "教龄不能为负数")
    @Max(value = 50, message = "教龄不能超过50年")
    private Integer coachTeachingYears;
}