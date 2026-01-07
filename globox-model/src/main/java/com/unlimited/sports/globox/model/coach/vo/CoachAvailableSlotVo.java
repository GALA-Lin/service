package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 供用户查询可预约时段VO
 */
@Data
@Builder
public class CoachAvailableSlotVo {
    /**
     * 时段模板ID
     */
    private Long coachSlotTemplateId;

    /**
     * 预约记录ID
     */
    private Long slotRecordId;
    /**
     * 预约日期
     */
    @NonNull
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    @NonNull
    private LocalTime startTime;

    private LocalTime endTime;

    /**
     * 时段长度（分钟）
     */
    private Integer durationMinutes;

    /**
     * 价格
     */
    private BigDecimal price;
    /**
     * 适用区域
     */
    private List<String> acceptableAreas;

    /**
     * 场地要求说明
     */
    private String venueRequirementDesc;

    /**
     * 服务类型
     */
    private Integer coachServiceType;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 时段状态
     */
    private Integer slotStatus;

    /**
     * 时段状态描述（前端友好展示）
     */
    private String slotStatusDesc;
}