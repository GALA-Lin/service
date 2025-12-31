package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @since 2025/12/29 11:56
 * 教练订单状态变更日志表
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_booking_status_logs")
public class CoachBookingStatusLogs implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志ID
     */
    @TableId(value = "coach_booking_status_logs_id", type = IdType.AUTO)
    private Long coachBookingStatusLogsId;

    /**
     * 订单ID
     */
    @TableField(value = "coach_booking_id")
    private Long coachBookingId;

    /**
     * 原状态
     */
    @TableField(value = "from_status")
    private Integer fromStatus;

    /**
     * 新状态
     */
    @TableField(value = "to_status")
    private Integer toStatus;

    /**
     * 操作人类型：1-用户，2-教练，3-系统
     */
    @TableField(value = "operator_type")
    private Integer operatorType;

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 备注说明
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

}
