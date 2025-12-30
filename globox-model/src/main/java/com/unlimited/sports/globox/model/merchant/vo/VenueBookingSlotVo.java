package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 预订时段视图（用于订单详情）
 * @since 2025-12-27
 * 展示订单中包含的具体时段信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueBookingSlotVo {

    /**
     * 槽位记录ID
     */
    @NonNull
    private Long bookingSlotId;

    /**
     * 关联订单ID
     */
    private Long orderId;

    /**
     * 场地ID
     */
    private Long courtId;

    /**
     * 场地名称
     */
    private String courtName;

    /**
     * 预订日期
     */
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 时段价格
     */
    private BigDecimal unitPrice;

    /**
     * 状态：1=可预订，2=占用中，3=不可预订
     */
    @NonNull
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 是否可取消
     */
    private Boolean cancelable;

    /**
     * 锁定类型：1=用户订单，2=商家锁场
     */
    private Integer lockedBy;

    /**
     * 锁定原因
     */
    private String lockReason;

    /**
     * 预订用户ID
     */
    private Long userId;
}