package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 时段可用性视图
 * @since 2025-12-27
 * 用于展示某个日期的时段列表及其可用状态
 */
@Data
@Builder
public class SlotAvailabilityVo {


    /**
     * 槽位模板ID
     */
    private Long templateId;

    /**
     * 槽位记录ID（如果已生成）
     */
    private Long bookingSlotId;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 状态码
     */
    private Integer status;

    /**
     * 是否可用
     */
    private Boolean available;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 状态说明
     */
    private String statusRemark;

    /**
     * 锁定类型（如果被锁定）
     */
    private Integer lockedType;

    /**
     * 锁定原因
     */
    private String lockReason;

    /**
     * 关联订单ID
     */
    private String orderId;

    /**
     * 锁场批次
     */
    private Long merchantBatchId;

    /**
     * 商家操作人姓名
     */
    private String displayName;

    /**
     * 使用人名称
     */
    private String userName;

    /**
     * 使用者电话
     */
    private String userPhone;

    // ========== 活动相关字段（新增） ==========

    /**
     * 槽位类型：1-普通槽位，2-活动槽位
     */
    private Integer slotType;


    /**
     * 活动名称
     */
    private String activityName;

    /**
     * 活动图片
     */
    private List<String> imageUrls;

    /**
     * 当前参与人数
     */
    private Integer currentParticipants;

    /**
     * 最大参与人数
     */
    private Integer maxParticipants;

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