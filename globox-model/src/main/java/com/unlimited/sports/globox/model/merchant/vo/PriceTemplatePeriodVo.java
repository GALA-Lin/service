package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 价格时段VO
 */
@Data
@Builder
public class PriceTemplatePeriodVo {

    /**
     * 时段ID
     */
    @NonNull
    private Long periodId;

    /**
     * 开始时间
     */
    @NonNull
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 工作日价格
     */
    private BigDecimal weekdayPrice;

    /**
     * 周末价格
     */
    private BigDecimal weekendPrice;

    /**
     * 节假日价格
     */
    private BigDecimal holidayPrice;

    /**
     * 是否启用
     */
    @NonNull
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
