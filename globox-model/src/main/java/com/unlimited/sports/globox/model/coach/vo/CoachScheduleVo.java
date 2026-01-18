package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 供教练查询自己日程VO
 */
@Data
@Builder
public class CoachScheduleVo {

    /**
     * 日程类型:平台日程/自定义日程
     */
    @NonNull
    private String scheduleType;

    /**
     * 日期
     */
    @NonNull
    private LocalDate scheduleDate;

    /**
     * 开始时间
     */
    @NonNull
    private LocalTime startTime;

    private LocalTime endTime;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 学生电话
     */
    private String studentPhone;

    /**
     * 场地名称
     */
    private String venue;
    /**
     * 课程类型
     */
    private Integer coachServiceType;
    /**
     * 备注
     */
    private String remark;

    /**
     * 订单ID（平台订单时）
     */
    private Long bookingId; // 订单ID（平台订单时）

    /**
     * 自定义日程ID
     */
    private Long customScheduleId;
}
