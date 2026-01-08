package com.unlimited.sports.globox.model.merchant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * @since 2025-12-18-11:18
 * 场馆营业时间表
 */
@Data
@TableName("venue_business_hours")
public class VenueBusinessHours implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 营业时间ID
     */
    @TableId(value = "business_hour_id", type = IdType.AUTO)
    private Long businessHourId;

    /**
     * 场馆ID
     */
    @TableField("venue_id")
    private Long venueId;

    /**
     * 规则类型：1=REGULAR，2=SPECIAL_DATE，3=CLOSED_DATE
     */
    @TableField("rule_type")
    private Integer ruleType;

    /**
     * 星期几 (1-7)，仅对REGULAR类型有效
     */
    @TableField("day_of_week")
    private Integer dayOfWeek;

    /**
     * 特定日期，仅对SPECIAL_DATE和CLOSED_DATE类型有效
     */
    @TableField("effective_date")
    private LocalDate effectiveDate;

    /**
     * 开门时间
     */
    @TableField("open_time")
    private LocalTime openTime;

    /**
     * 关门时间
     */
    @TableField("close_time")
    private LocalTime closeTime;

    /**
     * 规则优先级（越大优先级越高）
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

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
