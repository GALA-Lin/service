package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 商家查询订单详情请求的数据传输对象。
 * 该类用于封装商家查询特定订单详情时所需的必要信息，包括订单号、商家ID和场地ID。
 */
@Data
@Builder
public class MerchantGetOrderDetailsRequestDto {
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "商家号不能为空")
    private Long merchantId;

    @NotNull(message = "场馆号不能为空")
    private Long venueId;
}
