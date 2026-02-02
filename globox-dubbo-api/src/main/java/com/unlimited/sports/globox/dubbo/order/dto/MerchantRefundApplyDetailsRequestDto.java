package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;


/**
 * 商家退款申请详情请求数据传输对象。
 * 该类用于封装商家在查询特定退款申请详情时所需的数据，包括退款申请ID、订单号、商家ID以及场馆ID等信息。
 * 此外，还提供了一个选项来决定是否返回与订单状态相关的日志信息。
 */
@Data
@Builder
public class MerchantRefundApplyDetailsRequestDto implements Serializable {

    @NotNull(message = "退款申请ID不能为空")
    private Long refundApplyId;

    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "场馆ID不能为空")
    private Long venueId;
}