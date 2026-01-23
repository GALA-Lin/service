package com.unlimited.sports.globox.model.merchant.vo;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 已锁定时段VO
 * @since 2025-12-28 10:50
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockedSlotVo {

    /**
     * 记录ID
     */
    @NonNull
    private Long recordId;

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
     * 锁定类型：1=用户订单，2=商家锁场
     */
    private Integer lockedBy;

    /**
     * 锁定类型名称
     */
    private String lockedByName;

    /**
     * 锁场原因（商家锁场时有值）
     */
    private String lockReason;

    /**
     * 关联订单号（用户订单锁定时有值）
     */
    private String orderNo;

    /**
     * 预订用户ID（用户订单锁定时有值）
     */
    private Long userId;

    /**
     * 预订用户昵称（用户订单锁定时有值）
     */
    private String userNickname;

    /**
     * 锁定时间
     */
    private LocalDateTime lockedAt;

    /**
     * 状态
     */
    @NonNull
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;
}