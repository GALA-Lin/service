package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-22:42
 * @Description: 营业时间视图
 */
@Data
public class VenueBusinessHoursVo {

    /**
     * 营业时间ID
     */
    @NonNull
    private Long businessHourId;

    /**
     * 场馆ID
     */
    @NonNull
    private Long venueId;

    /**
     * 规则类型
     */
    private Integer ruleType;

    /**
     * 规则类型名称
     */
    private String ruleTypeName;

    /**
     * 星期几
     */
    private Integer dayOfWeek;

    /**
     * 星期名称
     */
    private String dayOfWeekName;

    /**
     * 特定日期
     */
    private LocalDate effectiveDate;

    /**
     * 开门时间
     */
    private LocalTime openTime;

    /**
     * 关门时间
     */
    private LocalTime closeTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 备注
     */
    private String remark;
}