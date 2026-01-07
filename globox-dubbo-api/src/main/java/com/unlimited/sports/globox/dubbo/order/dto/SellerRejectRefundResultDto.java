package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商家拒绝退款结果的数据传输对象。
 * 该类用于封装商家对退款申请的处理结果，特别是当商家决定拒绝退款时的相关信息。
 * 包含订单号、退款申请ID、退款申请状态、审核时间、订单状态及名称，以及被拒绝的item数量等字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRejectRefundResultDto implements Serializable {

    @NotNull
    private Long orderNo;
    @NotNull
    private Long refundApplyId;

    @NotNull
    private ApplyRefundStatusEnum applyStatus;
    @NotNull
    private LocalDateTime reviewedAt;

    @NotNull
    private OrderStatusEnum orderStatus;
    @NotNull
    private String orderStatusName;

    /**
     * 本次拒绝影响的 item 数量（通常等于 apply_items 数量）
     */
    @NotNull
    private Integer rejectedItemCount;
}