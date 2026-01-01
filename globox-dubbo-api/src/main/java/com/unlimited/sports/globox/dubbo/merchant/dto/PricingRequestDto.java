package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单前查询价格 - 请求 DTO
 *
 * @author dk
 * @since 2025/12/22 17:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PricingRequestDto implements Serializable {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 预定日期
     */
    private LocalDate bookingDate;

    /**
     * 预定的槽列表
     */
    private List<Long> slotIds;
}
