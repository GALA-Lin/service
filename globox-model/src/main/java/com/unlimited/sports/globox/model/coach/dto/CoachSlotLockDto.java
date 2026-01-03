package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 时段锁定DTO（用户下单）
 */
@Data
public class CoachSlotLockDto {

    @NotNull(message = "时段记录ID不能为空")
    private Long slotRecordId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 锁定分钟数，默认15分钟
     */
    @Min(value = 5, message = "锁定时间至少5分钟")
    @Max(value = 30, message = "锁定时间最多30分钟")
    private Integer lockMinutes = 15;
}
