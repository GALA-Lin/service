package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 商家拒绝退款请求的数据传输对象。
 * 该类用于封装商家处理退款申请时所需的必要信息，包括订单号、退款申请ID等，
 * 并允许商家提供拒绝原因备注和拒绝原因码以供后续分析或风控使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRejectRefundRequestDto implements Serializable {

    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "退款申请ID不能为空")
    private Long refundApplyId;

    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    /**
     * 可选：拒绝原因备注（给用户/运营看）
     */
    private String remark;
}