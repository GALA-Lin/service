package com.unlimited.sports.globox.model.merchant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * @since  2025-12-18-22:35
 * 时段可用性查询DTO
 */
@Data
public class SlotAvailabilityQueryDto {

    /**
     * 场地ID
     */
    @NotNull(message = "场地ID不能为空")
    private Long courtId;

    /**
     * 查询日期
     */
    @NotNull(message = "查询日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bookingDate;
}
