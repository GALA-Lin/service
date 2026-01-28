package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @since 2026/1/26 16:27
 *
 */
@Data
@Builder
public class MerchantGetOrderDetailsDto {
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "场馆号不能为空")
    private Long venueId;
}
