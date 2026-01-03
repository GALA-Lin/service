package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @since 2026/1/3 14:39
 * 教练时段操作日志
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_slot_batch_operation_log")
public class CoachSlotBatchOperationLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 操作日志ID
     */
    @TableId(type = IdType.AUTO)
    private Long operationLogId;

    /**
     * 教练ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 操作类型：1=批量初始化 2=批量锁定 3=批量解锁 4=批量删除
     */
    @TableField(value = "operation_type")
    private Integer operationType;

    /**
     * 操作日期范围-开始
     */
    @TableField(value = "date_range_start")
    private LocalDate dateRangeStart;

    /**
     * 操作日期范围-结束
     */
    @TableField(value = "date_range_end")
    private LocalDate dateRangeEnd;

    /**
     * 操作时间范围-开始
     */
    @TableField(value = "time_range_start")
    private LocalTime timeRangeStart;

    /**
     * 操作时间范围-结束
     */
    @TableField(value = "time_range_end")
    private LocalTime timeRangeEnd;

    /**
     * 影响的记录数
     */
    @TableField(value = "affected_count")
    private Integer affectedCount;

    /**
     * 操作原因
     */
    @TableField(value = "operation_reason")
    private String operationReason;

    /**
     * 操作参数（记录具体操作内容）
     */
    @TableField(value = "operation_params")
    private String operationParams;

    /**
     * 操作人ID
     */
    @TableField(value = "operator_id")
    private Long operatorId;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
