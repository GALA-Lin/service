package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.UserRefundReasonEnum;
import com.unlimited.sports.globox.model.order.vo.RefundTimelineVo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家退款申请详情结果数据传输对象。
 * 该类用于封装商家在处理退款申请时所需的详细信息，包括订单基本信息、退款申请状态、退款金额等。
 * 此外，还包含了与订单相关的项目和额外费用的退款明细。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundApplyDetailsResultDto implements Serializable {

    @NotNull
    private Long orderNo;
    @NotNull
    private Long refundApplyId;

    @NotNull
    private Long userId;

    @NotNull
    private Long venueId;
    @NotNull
    private String venueName;

    @NotNull
    private OrderStatusEnum orderStatus;

    @NotNull
    private ApplyRefundStatusEnum applyStatus;

    @NotNull
    private UserRefundReasonEnum reasonCode;
    @Null
    private String reasonDetail;

    @NotNull
    private LocalDateTime appliedAt;
    @Null
    private LocalDateTime reviewedAt;
    @Null
    private String sellerRemark;

    @Null
    private LocalDateTime refundInitiatedAt;
    @Null
    private LocalDateTime refundCompletedAt;
    @Null
    private String refundTransactionId;

    /**
     * 汇总金额
     */
    @NotNull
    private BigDecimal totalRefundAmount;
    @NotNull
    private BigDecimal refundedAmount;
    @NotNull
    private BigDecimal refundingAmount;

    /**
     * 本次申请涉及的订单项（由 order_refund_apply_items 关联出）
     * - 每个 item 的 refundStatus 直接取 order_items.refund_status
     */
    @NotNull
    private List<MerchantRefundItemDto> items;

    /**
     * 本次申请涉及的额外费用退款明细（来自 order_refund_extra_charges）
     * - 这里不存 refundStatus（你要求去掉）
     * - 需要展示状态时：在 service 里用 orderItemId->item.refundStatus 推导；order级的用 orderStatus 推导
     */
    @NotNull
    private List<MerchantRefundExtraChargeDto> extraCharges;
}