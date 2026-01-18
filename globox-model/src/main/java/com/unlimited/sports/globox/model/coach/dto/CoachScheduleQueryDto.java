package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 教练日程查询DTO
 */
@Data
public class CoachScheduleQueryDto {

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
     * 备注
     */
    private String remark;

    /**
     * 是否包含自定义日程，默认true
     */
    private Boolean includeCustomSchedule = true;
}
