package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @since 2026/1/27
 * 更新时段记录和备注DTO
 */
@Data
public class UpdateCoachSlotVenueDto {

    /**
     * 时段记录ID
     */
    @NotNull(message = "时段记录ID不能为空")
    private Long slotRecordId;

    /**
     * 教练用户ID（从请求头获取，用于权限验证）
     */
    private Long coachUserId;

    /**
     * 场地名称
     */
    @Size(max = 50, message = "场地名称不能超过50字")
    private String venue;


    /**
     * 备注说明
     */
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}