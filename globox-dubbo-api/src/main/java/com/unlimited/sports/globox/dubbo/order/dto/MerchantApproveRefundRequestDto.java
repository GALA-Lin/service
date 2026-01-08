package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dubbo.common.lang.Nullable;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家同意退款请求的数据传输对象。
 * 该类用于封装商家处理退款申请时所需的必要信息，包括订单号、退款申请ID等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApproveRefundRequestDto implements Serializable {

    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    @NotNull(message = "退款申请ID不能为空")
    private Long refundApplyId;

    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    @NotNull(message = "商家ID不能为空")
    private Long merchantId;

    @NotNull(message = "退款比例不能为空")
    private BigDecimal refundPercentage;

    /**
     * 可选：商家备注
     */
    @Nullable
    private String remark;
}