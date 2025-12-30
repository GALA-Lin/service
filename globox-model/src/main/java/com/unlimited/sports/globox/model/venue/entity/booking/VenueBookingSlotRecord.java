package com.unlimited.sports.globox.model.venue.entity.booking;

import com.baomidou.mybatisplus.annotation.*;
import com.unlimited.sports.globox.model.venue.enums.OperatorSourceEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实际的槽位操作记录,带上具体的日期,记录具体的占用
 *
 */
@Data
@TableName("venue_booking_slot_record")

public class VenueBookingSlotRecord {
    @TableId(type = IdType.ASSIGN_ID)
    private Long bookingSlotRecordId;


    /**
     * 绑定的slot模版id,指向具体的时间段
     */
    private Long slotTemplateId;


    /**
     * 槽位状态：1=AVAILABLE(可预订) 2=LOCKED_IN(占用中/锁定) 3=UNAVAILABLE(不可预定)
     */
    private int status;


    /**
     * 预订日期
     */
    private LocalDateTime bookingDate;

    /**
     * 操作人ID（用户ID或商家ID）
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作人来源：1=MERCHANT(商家端) 2=USER(用户端)
     */
    @TableField("operator_source")
    private OperatorSourceEnum operatorSource;

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
