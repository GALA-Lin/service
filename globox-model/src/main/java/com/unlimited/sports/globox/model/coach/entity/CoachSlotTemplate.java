package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @since 2026/1/3 14:33
 * 教练时段模板
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_slot_template")
public class CoachSlotTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时段模板ID
     */
    @TableId(type = IdType.AUTO)
    private Long coachSlotTemplateId;

    /**
     * 教练ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 开始时间(HH:mm)
     */
    @TableField(value = "start_time")
    private LocalTime startTime;

    /**
     * 结束时间(HH:mm)
     */
    @TableField(value = "end_time")
    private LocalTime endTime;

    /**
     * 时长（分钟）
     */
    @TableField(value = "duration_minutes")
    private Integer durationMinutes;

    /**
     * 1-一对一教学，2-一对一陪练，3-一对二，4-小班(3-6人)（NULL表示该时段所有服务都可约）
     */
    @TableField(value = "coach_service_id")
    private Integer coachServiceId;

    /**
     * 该时段单价（元）
     */
    @TableField(value = "price")
    private BigDecimal price;

    /**
     * 可接受区域列表：["成都高新区","成都武侯区"]
     */
    @TableField(value = "acceptable_areas")
    private String acceptableAreas;

    /**
     * 场地要求说明
     */
    @TableField(value = "venue_requirement_desc")
    private String venueRequirementDesc;

    /**
     * 提前开放预约的天数，默认7天
     */
    @TableField(value = "advance_booking_days")
    private Integer advanceBookingDays;

    /**
     * 是否删除：0=未删除 1=已删除
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

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
}
