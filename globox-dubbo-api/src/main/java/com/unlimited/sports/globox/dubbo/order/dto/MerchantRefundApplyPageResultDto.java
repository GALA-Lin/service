package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家退款申请页面结果的数据传输对象。
 * 该类封装了商家处理退款申请时所需的信息，包括退款申请ID、订单号、用户ID、场地ID、订单状态、退款申请状态等。
 * 同时还记录了退款原因代码及详情、申请时间和审核时间等信息。
 * 此DTO主要用于在系统内部传递商家退款申请的相关数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundApplyPageResultDto implements Serializable {

    @NotNull
    private Long refundApplyId;
    @NotNull
    private Long orderNo;

    @NotNull
    private Long userId;

    @NotNull
    private Long venueId;

    @NotNull
    private OrderStatusEnum orderStatus;

    @NotNull
    private ApplyRefundStatusEnum applyStatus;

    @NotNull
    private Integer reasonCode;
    @Null
    private String reasonDetail;

    @NotNull
    private LocalDateTime appliedAt;
    @Null
    private LocalDateTime reviewedAt;

    /**
     * 本次申请包含的退款项数量（order_refund_apply_items count）
     */
    @NotNull
    private Integer applyItemCount;

    /**
     * 展示用：本次申请“应退总额”（口径：item金额 + extraCharges金额 - fee）
     */
    @NotNull
    private BigDecimal totalRefundAmount;
}