package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * @since 2025-12-18-22:33
 * 营业时间规则Dto
 */
@Data
public class VenueBusinessHoursDto {

    /**
     * 规则类型：1=REGULAR，2=SPECIAL_DATE，3=CLOSED_DATE
     */
    @NotNull(message = "规则类型不能为空")
    private Integer ruleType;

    /**
     * 星期几 (1-7)，仅REGULAR类型需要
     */
    @Min(value = 1, message = "星期必须在1-7之间")
    @Max(value = 7, message = "星期必须在1-7之间")
    private Integer dayOfWeek;

    /**
     * 特定日期，SPECIAL_DATE和CLOSED_DATE类型需要
     */
    private LocalDate effectiveDate;

    /**
     * 开门时间（CLOSED_DATE类型不需要）
     */
    private LocalTime openTime;

    /**
     * 关门时间（CLOSED_DATE类型不需要）
     */
    private LocalTime closeTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注长度不能超过255字符")
    private String remark;
}
