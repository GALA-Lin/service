package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 第三方平台槽位统一DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartySlotDto {

    /**
     * 开始时间（格式：HH:mm 或 HH:mm:ss）
     */
    private String startTime;

    /**
     * 结束时间（格式：HH:mm 或 HH:mm:ss）
     */
    private String endTime;

    /**
     * 是否可预订
     */
    private Boolean available;

    /**
     * 槽位价格
     */
    private BigDecimal price;
}
