package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 槽位模板实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_booking_slot_template")
public class VenueBookingSlotTemplate {

    /**
     * 槽位模板ID
     */
    @TableId(value = "booking_slot_template_id", type = IdType.AUTO)
    private Long bookingSlotTemplateId;

    /**
     * 场地ID
     */
    @TableField("court_id")
    private Long courtId;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 是否删除：0=未删除 1=已删除
     */
    @TableField("is_deleted")
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