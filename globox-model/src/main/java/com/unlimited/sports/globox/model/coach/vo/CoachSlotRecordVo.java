package com.unlimited.sports.globox.model.coach.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * @since 2026/1/3 13:16
 * 用户端展示时段模板时段记录Vo
 */
@Data
@Builder
public class CoachSlotRecordVo {

    /**
     * 时段记录ID
     */
    private Long slotRecordId;

    /**
     * 模板ID
     */
    @NonNull
    private Long templateId;

    /**
     * 日期
     */
    private LocalDate bookingDate;

    /**
     * 开始时间
     */
    @NonNull
    private LocalTime startTime;

    private LocalTime endTime;

    private Integer durationMinutes;

    /**
     * 时段状态：1=LOCKED(锁定中/下单中) 2=UNAVAILABLE(不可预约/教练关闭) 3=CUSTOM_EVENT(自定义日程占用)
     */
    private Integer status;

    /**
     * 教练姓名
     */
    private String coachName;

    /**
     * 时段状态描述
     */
    private String statusDesc;

    /**
     * 时段价格
     */
    private BigDecimal price;

    /**
     * 时段可用区域
     */
    private List<String> acceptableAreas;

    /**
     * 场地要求说明
     */
    private String venueRequirementDesc;

    /**
     * 关联预约ID
     */
    private Long bookingId;
    /**
     * 锁定原因
     */
    private String lockReason;
    /**
     * 锁定到期时间
     */
    private LocalDateTime lockedUntil;
}
