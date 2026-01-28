package com.unlimited.sports.globox.model.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @since 2026/1/26 16:35
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantCancelOrderDto implements Serializable {

    /**
     * 订单号
     */
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    /**
     * Venue ID（场馆）
     */
    @NotNull(message = "Venue ID不能为空")
    private Long venueId;

}
