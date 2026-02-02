package com.unlimited.sports.globox.venue.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Away槽位价格信息
 * 包含单个槽位的开始时间、价格和第三方场地ID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwaySlotPrice {

    /**
     * 槽位开始时间
     */
    private LocalTime startTime;

    /**
     * 槽位价格
     */
    private BigDecimal price;

    /**
     * 第三方场地ID
     */
    private String thirdPartyCourtId;
}
