package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @since 2025/12/29 11:51
 * 教练预约时段表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_service_slots")
public class CoachServiceSlots implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时段ID
     */
    @TableId(value = "coach_service_slots_id", type = IdType.AUTO)
    private Long coachServiceSlotsId;

    /**
     * 教练ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 日期
     */
    @TableField(value = "coach_service_date")
    private LocalDate coachServiceDate;

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
     * 指定服务ID（NULL表示该时段所有服务都可约）
     */
    @TableField(value = "coach_service_id")
    private Long coachServiceId;

    /**
     * 时段状态：1-可预约，2-已锁定（下单中），3.不可预约（已预约/时段关闭）
     */
    @TableField(value = "coach_service_slot_status")
    private Integer coachServiceSlotStatus;

    /**
     * 锁定截止时间（下单未支付时使用）
     */
    @TableField(value = "coach_locked_until")
    private LocalDateTime coachLockedUntil;

    /**
     * 锁定该时段的用户ID
     */
    @TableField(value = "coach_locked_by_user_id")
    private Long coachLockedByUserId;

    /**
     * 场地要求说明
     */
    @TableField(value = "venue_requirement")
    private String venueRequirement;

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