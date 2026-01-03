package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @since 2026/1/3 14:37
 * 教练自定义日程
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_custom_schedule")
public class CoachCustomSchedule  implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long coachCustomScheduleId;
    /**
     * 教练ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 学员姓名
     */
    @TableField(value = "student_name")
    private String studentName;

    /**
     * 日程日期
     */
    @TableField(value = "schedule_date")
    private LocalDate scheduleDate;

    /**
     * 开始时间
     */
    @TableField(value = "start_time")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @TableField(value = "end_time")
    private LocalTime endTime;

    /**
     * 时长（分钟）
     */
    @TableField(value = "duration_minutes")
    private Integer durationMinutes;

    /**
     * 上课地点
     */
    @TableField(value = "venue_name")
    private String venueName;

    /**
     * 详细地址
     */
    @TableField(value = "venue_address")
    private String venueAddress;

    /**
     * 1-一对一教学，2-一对一陪练，3-一对二，4-小班(3-6人)
     */
    @TableField(value = "course_type")
    private Integer courseType;

    /**
     * 提前提醒时间（分钟），NULL表示不提醒
     */
    @TableField(value = "reminder_minutes")
    private Integer reminderMinutes;

    /**
     * 状态：1=正常 2=已取消 3=已完成
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 价格（元）
     */
    @TableField(value = "coach_price")
    private BigDecimal coachPrice;

    /**
     * 备注说明
     */
    @TableField(value = "remark")
    private String remark;
}