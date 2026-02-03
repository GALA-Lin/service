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
     * 日程类型: PLATFORM_SLOT(平台预约) / CUSTOM_EVENT(自定义日程)
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

    /**
     * 结束时间
     */
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
     * 课程类型：1-一对一教学，2-一对一陪练，3-一对二，4-小班(3-6人)
     */
    private Integer coachServiceType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 订单ID（平台订单时）
     */
    private Long bookingId;

    /**
     * 自定义日程ID
     */
    private Long customScheduleId;

    /**
     * 锁定该时段的用户ID（平台课程时）
     */
    private Long lockedByUserId;

    /**
     * 课程状态：1=待上课，2=上课中，3=已完成，4=已取消
     * 动态计算，不存储在数据库
     */
    private Integer courseStatus;

    /**
     * 课程状态描述
     * 动态计算，不存储在数据库
     */
    private String courseStatusDesc;
}