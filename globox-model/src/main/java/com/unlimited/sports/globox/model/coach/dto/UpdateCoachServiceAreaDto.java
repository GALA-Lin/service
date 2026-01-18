package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @since 2026/1/12
 * 更新教练服务区域DTO
 */
@Data
public class UpdateCoachServiceAreaDto {

    /**
     * 教练用户ID（从请求头获取）
     */
    private Long coachUserId;

    /**
     * 常驻服务区域
     */
    @NotBlank(message = "常驻服务区域不能为空")
    @Size(max = 500, message = "服务区域不能超过500字")
    private String coachServiceArea;

    /**
     * 常驻区域最低授课时长（小时）
     */
    @Min(value = 0, message = "最低授课时长至少0小时")
    @Max(value = 24, message = "最低授课时长不能超过24小时")
    private Integer coachMinHours;

    /**
     * 可接受的远距离服务区域
     */
    @Size(max = 500, message = "远距离服务区域不能超过500字")
    private String coachRemoteServiceArea;

    /**
     * 远距离区域最低授课时长（小时）
     */
    @Min(value = 0, message = "最低授课时长至少0小时")
    @Max(value = 24, message = "最低授课时长不能超过24小时")
    private Integer coachRemoteMinHours;
}