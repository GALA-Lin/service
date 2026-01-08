package com.unlimited.sports.globox.model.venue.entity.booking;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;


/**
 * slot模板表
 * 只记录开始时间,结束时间,如8:00-8:30,而不指定具体日期
 */
@Data
@Builder
@TableName("venue_booking_slot_template")
public class VenueBookingSlotTemplate {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long bookingSlotTemplateId;


    /**
     * 对应的场地id
     */
    private Long courtId;

    /**
     * 开始时间(HH:mm),因此使用LocalTime
     */
    private LocalTime startTime;


    private LocalTime endTime;

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
