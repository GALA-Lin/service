package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Away球场价格信息DTO
 * 包含槽位价格、额外费用等信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwayPricingDto {

    /**
     * 按时间点的槽位价格
     * key: 槽位开始时间，value: 价格
     */
    private Map<LocalTime, BigDecimal> slotPrices;


    /**
     * 订单总价
     */
    private BigDecimal totalPrice;
}
