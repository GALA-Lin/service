package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商家退款项的数据传输对象。
 * 该类用于封装商家处理退款时涉及的订单项信息，包括订单项ID、场地ID、时段ID以及退款状态等。
 * 同时，还记录了与退款相关的金额细节，如商品金额、额外费用、退款手续费和实际退款金额。
 * 此DTO主要用于在系统内部传递商家退款请求及处理结果的相关数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundItemDto implements Serializable {

    @NotNull
    private Long orderItemId;
    @NotNull
    private Long venueId;
    @NotNull
    private Long recordId;

    @NotNull
    private RefundStatusEnum refundStatus;

    /**
     * 退款事实（order_item_refunds），可能为空（例如刚申请未审批）
     */
    @NotNull
    private BigDecimal itemAmount;
    @NotNull
    private BigDecimal extraChargeAmount;
    @Null
    private BigDecimal refundFee;
    @Null
    private BigDecimal refundAmount;
}