package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 时段可用性视图
 * @since 2025-12-27
 * 用于展示某个日期的时段列表及其可用状态
 */
@Data
@Builder
public class SlotAvailabilityVo {

    /**
     * 槽位记录ID（如果已生成记录）
     */

    private Long bookingSlotId;

    /**
     * 槽位模板ID
     */

    private Long templateId;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 是否可预订
     */
    private Boolean available;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 状态码：1=可预订，2=占用中，3=不可预订
     */
    private Integer status;

    /**
     * 状态说明
     */
    private String statusRemark;

    /**
     * 锁定类型：1=用户订单，2=商家锁场，null=未锁定
     */
    private Integer lockedType;

    /**
     * 锁定原因（商家锁场时显示）
     */
    private String lockReason;

    /**
     * 关联订单ID（用户订单占用时显示）
     */
    private String orderId;

    /**
     * 快速构建可用时段
     */
    public static SlotAvailabilityVo available(Long recordId, Long templateId,
                                               LocalTime startTime, LocalTime endTime,
                                               BigDecimal price) {
        return SlotAvailabilityVo.builder()
                .bookingSlotId(recordId)
                .templateId(templateId)
                .startTime(startTime)
                .endTime(endTime)
                .available(true)
                .price(price)
                .status(1)
                .statusRemark("可预订")
                .build();
    }

    /**
     * 快速构建不可用时段
     */
    public static SlotAvailabilityVo unavailable(Long recordId, Long templateId,
                                                 LocalTime startTime, LocalTime endTime,
                                                 String reason) {
        return SlotAvailabilityVo.builder()
                .bookingSlotId(recordId)
                .templateId(templateId)
                .startTime(startTime)
                .endTime(endTime)
                .available(false)
                .price(BigDecimal.ZERO)
                .status(3)
                .statusRemark(reason)
                .build();
    }

    /**
     * 快速构建未开放时段
     */
    public static SlotAvailabilityVo notGenerated(Long templateId,
                                                  LocalTime startTime, LocalTime endTime) {
        return SlotAvailabilityVo.builder()
                .templateId(templateId)
                .startTime(startTime)
                .endTime(endTime)
                .available(false)
                .price(BigDecimal.ZERO)
                .statusRemark("未开放")
                .build();
    }
}