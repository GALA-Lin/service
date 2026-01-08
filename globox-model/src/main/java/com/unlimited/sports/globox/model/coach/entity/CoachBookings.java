package com.unlimited.sports.globox.model.coach.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * @since 2025/12/29 11:55
 * 教练预约订单表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "coach_bookings")
public class CoachBookings implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @TableId(value = "coach_bookings_id", type = IdType.AUTO)
    private Long coachBookingsId;

    /**
     * 订单号
     */
    @TableField(value = "coach_bookings_order_no")
    private String coachBookingsOrderNo;

    /**
     * 预约用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 教练ID
     */
    @TableField(value = "coach_user_id")
    private Long coachUserId;

    /**
     * 选择的服务ID
     */
    @TableField(value = "coach_service_id")
    private Long coachServiceId;

    /**
     * 关联的时段ID（可能为NULL，如教练删除时段）
     */
    @TableField(value = "coach_service_slot_id")
    private Long coachServiceSlotId;

    /**
     * 预约日期
     */
    @TableField(value = "coach_booking_date")
    private LocalDate coachBookingDate;

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
     * 时长（分钟）
     */
    @TableField(value = "duration")
    private Integer duration;

    /**
     * 原价
     */
    @TableField(value = "coach_booking_original_price")
    private BigDecimal coachBookingsOrderOriginalPrice;

    /**
     * 优惠金额
     */
    @TableField(value = "coach_booking_discount_amount")
    private BigDecimal coachBookingsOrderDiscountAmount;

    /**
     * 实付金额
     */
    @TableField(value = "coach_booking_final_price")
    private BigDecimal coachBookingsOrderFinalPrice;

    /**
     * 球场ID
     */
    @TableField(value = "venue_id")
    private Long venueId;

    /**
     * 球场名称（冗余字段）
     */
    @TableField(value = "venue_name")
    private String venueName;

    /**
     * 场地号
     */
    @TableField(value = "court_number")
    private String courtNumber;

    /**
     * 联系人姓名
     */
    @TableField(value = "contact_name")
    private String contactName;

    /**
     * 联系电话
     */
    @TableField(value = "contact_phone")
    private String contactPhone;

    /**
     * 学员人数
     */
    @TableField(value = "student_count")
    private Integer studentCount;

    /**
     * 特殊需求说明
     */
    @TableField(value = "special_requirements")
    private String specialRequirements;

    /**
     * 订单状态：1-待支付，2-待确认，3-已确认，4-进行中，5-已完成，6-已取消，7-已退款
     */
    @TableField(value = "coach_booking_status")
    private Integer coachBookingStatus;

    /**
     * 支付状态：0-未支付，1-已支付，2-部分退款，3-全额退款
     */
    @TableField(value = "payment_status")
    private Integer paymentStatus;

    /**
     * 支付方式：wechat，alipay等
     */
    @TableField(value = "payment_method")
    private String paymentMethod;

    /**
     * 支付时间
     */
    @TableField(value = "payment_time")
    private LocalDateTime paymentTime;

    /**
     * 支付流水号
     */
    @TableField(value = "payment_transaction_id")
    private String paymentTransactionId;

    /**
     * 确认方：1-教练确认，2-自动确认
     */
    @TableField(value = "confirmed_by")
    private Integer confirmedBy;

    /**
     * 确认时间
     */
    @TableField(value = "confirmed_at")
    private LocalDateTime confirmedAt;

    /**
     * 取消方：1-用户，2-教练，3-系统
     */
    @TableField(value = "cancelled_by")
    private Integer cancelledBy;

    /**
     * 取消原因
     */
    @TableField(value = "cancel_reason")
    private String cancelReason;

    /**
     * 取消时间
     */
    @TableField(value = "cancelled_at")
    private LocalDateTime cancelledAt;

    /**
     * 完成时间
     */
    @TableField(value = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 是否已评价：0-未评价，1-已评价
     */
    @TableField(value = "is_reviewed")
    private Integer isReviewed;

    /**
     * 创建时间（下单时间）
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 订单过期时间（待支付状态）
     */
    private LocalDateTime expiresAt;

}
