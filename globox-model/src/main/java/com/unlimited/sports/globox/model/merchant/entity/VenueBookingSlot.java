package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * @since  2025-12-18-11:23
 * 预订时间槽位表
 */
@Data
@TableName("venue_booking_slot")
public class VenueBookingSlot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 预订槽位ID，主键
     */
    @TableId(value = "booking_slot_id", type = IdType.AUTO)
    private Long bookingSlotId;

    /**
     * 关联订单ID（禁用时段为NULL）
     */
    @TableField("order_id")
    private Long orderId;

    /**
     * 场地ID
     */
    @TableField("court_id")
    private Long courtId;

    /**
     * 预订日期
     */
    @TableField("booking_date")
    private LocalDate bookingDate;

    /**
     * 预订开始时间（08:00, 08:30, 09:00...）
     */
    @TableField("start_time")
    private LocalTime startTime;

    /**
     * 预订结束时间
     */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 预订用户ID（禁用时段为NULL）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 支付生命周期状态：
     * 1: PENDING_PAY - 订单已生成，用户支付中
     * 2: PAID - 用户已支付
     * 3: COMPLETED - 已完成预订
     * 4: BLOCKED - 禁用
     * 5: CANCELLED - 已取消
     */
    @TableField("status")
    private Integer status;

    /**
     * 预订来源：1=home平台，2=away平台
     */
    @TableField("source")
    private Integer source;


    /**
     * 订槽可见性时间
     */
    @TableField("visibility_time")
    private LocalDateTime visibilityTime;
    /**
     * 订单时的价格
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 支付截止时间（PENDING_PAY状态下有效）
     */
    @TableField("pay_deadline")
    private LocalDateTime payDeadline;

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