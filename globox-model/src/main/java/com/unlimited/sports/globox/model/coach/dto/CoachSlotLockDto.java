package com.unlimited.sports.globox.model.coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 时段锁定DTO（用户下单）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachSlotLockDto {

    /**
     * 时段记录ID(已有记录时使用)
     */
    private Long slotRecordId;

    /**
     * 模板ID(按需创建时使用)
     */
    private Long templateId;

    /**
     * 预约日期(按需创建时使用)
     */
    private LocalDate bookingDate;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 锁定分钟数,默认15分钟
     */
    @Min(value = 5, message = "锁定时间至少5分钟")
    @Max(value = 30, message = "锁定时间最多30分钟")
    @Builder.Default
    private Integer lockMinutes = 15;
}
