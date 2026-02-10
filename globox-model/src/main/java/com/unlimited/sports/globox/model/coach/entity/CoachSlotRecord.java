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
 * @since 2026/1/3 14:36
 * 教练时段记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_slot_record")

public class CoachSlotRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时段记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long coachSlotRecordId;

    /**
     * 时段模板ID
     */
    @TableField(value = "coach_slot_template_id")
    private Long coachSlotTemplateId;

    /**
     * 教练ID（冗余字段，方便查询）
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 预约日期
     */
    @TableField(value = "booking_date")
    private LocalDate bookingDate;

    /**
     * 开始时间（冗余字段）
     */
    @TableField(value = "start_time")
    private LocalTime startTime;

    /**
     * 结束时间（冗余字段）
     */
    @TableField(value = "end_time")
    private LocalTime endTime;


    /**
     * 场地名称（球场名称）
     */
    @TableField(value = "venue")
    private String venue;

    /**
     * 订单备注（学员/教练填写的特殊需求）
     */
    @TableField(value = "remark")
    private String remark;


    /**
     * 时段状态：1=LOCKED(锁定中/下单中) 2=UNAVAILABLE(不可预约/教练关闭) 3=CUSTOM_EVENT(自定义日程占用)
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 关联的订单ID
     */
    @TableField(value = "coach_booking_id")
    private Long coachBookingId;

    /**
     * 锁定该时段的用户ID（status=2时）
     */
    @TableField(value = "locked_by_user_id")
    private Long lockedByUserId;

    /**
     * 锁定截止时间（下单未支付时使用）
     */
    @TableField(value = "locked_until")
    private LocalDateTime lockedUntil;

    /**
     * 锁定类型：1=用户下单锁定，2=教练手动锁定
     */
    @TableField(value = "locked_type")
    private Integer lockedType;

    /**
     * 锁定原因（教练手动锁定时填写）
     */
    @TableField(value = "lock_reason")
    private String lockReason;

    /**
     * 自定义日程ID（status=5时）
     */
    @TableField(value = "custom_schedule_id")
    private Long customScheduleId;

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 操作来源：1=教练端 2=用户端 3=系统自动
     */
    @TableField(value = "operator_source")
    private Integer operatorSource;

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
     * 逻辑删除 0表示未删除，1表示已删除
     */
    @TableLogic(value = "0", delval = "1")
    @TableField(value = "is_deleted")
    private Integer isDeleted;
}
