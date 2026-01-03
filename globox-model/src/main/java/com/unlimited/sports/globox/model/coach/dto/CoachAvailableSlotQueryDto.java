package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 可预约时段查询DTO
 */
@Data
public class CoachAvailableSlotQueryDto {

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
     * 服务类型
     */
    private Integer coachServiceType;
}
