package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 时段冲突VO
 */
@Data
@Builder
public class CoachSlotConflictVo {
    /**
     * 日期
     */
    @NonNull
    private LocalDate date;

    /**
     * 开始时间
     */
    @NonNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 冲突原因描述
     */
    private String conflictReason;

    /**
     * 相关ID（订单ID或自定义日程ID）
     */
    private Long relatedId;
}
