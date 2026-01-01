package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantCancelOrderRequestDto implements Serializable {

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

    /**
     * 商家 ID
     */
    @NotNull(message = "商家 ID 不能为空")
    private Long merchantId;
}