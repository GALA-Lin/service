package com.unlimited.sports.globox.model.merchant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 价格时段DTO
 */
@Data
public class PriceTemplatePeriodDto {

    /**
     * 时段ID（更新时需要，创建时不需要）
     */
    private Long periodId;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /**
     * 工作日价格
     */
    @NotNull(message = "工作日价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal weekdayPrice;

    /**
     * 周末价格
     */
    @NotNull(message = "周末价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal weekendPrice;

    /**
     * 节假日价格
     */
    @NotNull(message = "节假日价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal holidayPrice;

    /**
     * 是否启用
     */
    private Boolean isEnabled = true;
}
