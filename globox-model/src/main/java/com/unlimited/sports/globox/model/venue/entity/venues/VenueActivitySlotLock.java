package com.unlimited.sports.globox.model.venue.entity.venues;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 活动槽位锁定表
 * 记录活动占用的槽位
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("venue_activity_slot_lock")
public class VenueActivitySlotLock {

    /**
     * 锁定记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long lockId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 被锁定的槽位模板ID
     */
    private Long slotTemplateId;

    /**
     * 预订日期
     */
    private LocalDate bookingDate;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
