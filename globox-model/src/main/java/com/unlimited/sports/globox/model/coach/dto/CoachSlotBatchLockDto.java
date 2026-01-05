package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 批量锁定时段DTO
 */
@Data
public class CoachSlotBatchLockDto {

    /**
     * 教练ID
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 开始时间
     */
    private LocalTime startTime; // 可选，筛选特定时间段
    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 锁定原因
     */
    @NotBlank(message = "锁定原因不能为空")
    @Size(max = 500, message = "锁定原因不能超过500字")
    private String lockReason;

    /**
     * 操作人ID
     */
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;
}
