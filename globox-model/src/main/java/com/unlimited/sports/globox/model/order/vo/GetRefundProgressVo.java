package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.UserRefundReasonEnum;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRefundProgressVo implements Serializable {

    @NotNull
    private Long orderNo;
    @NotNull
    private Long refundApplyId;

    /**
     * 订单状态
     */
    @NotNull
    private OrderStatusEnum orderStatus;
    @NotNull
    private String orderStatusName;

    /**
     * 申请单状态：PENDING / APPROVED / REJECTED
     */
    @NotNull
    private ApplyRefundStatusEnum applyStatus;
    @NotNull
    private String applyStatusName;

    /**
     * 申请信息
     */
    @NotNull
    private UserRefundReasonEnum reasonCode;
    @Null
    private String reasonDetail;

    @Null
    private LocalDateTime appliedAt;
    @Null
    private LocalDateTime reviewedAt;
    @Null
    private String sellerRemark;

    /**
     * 支付平台退款过程
     */
    @Null
    private LocalDateTime refundInitiatedAt;
    @Null
    private LocalDateTime refundCompletedAt;
    @Null
    private String refundTransactionId;

    /**
     * 汇总金额
     * 申请/审批口径的应退总额（含 item + extra）
     */
    @NotNull
    private BigDecimal totalRefundAmount;
    /**
     * 已完成退款总额
     */
    @NotNull
    private BigDecimal refundedAmount;
    /**
     * 处理中金额
     */
    @NotNull
    private BigDecimal refundingAmount;

    /**
     * 本次申请涉及的订单项进度
     */
    @NotNull
    private List<RefundItemProgressVo> items;

    /**
     * 本次申请涉及的额外费用退款明细（item级 + 订单级；来自 order_refund_extra_charges）
     */
    @Null
    private List<RefundExtraChargeProgressVo> extraCharges;

    /**
     * 时间线（从 order_status_logs 挑 action=REFUND_APPLY/REFUND_APPROVE/REFUND_REJECT/REFUND_COMPLETE 等）
     */
    @Null
    private List<RefundTimelineVo> timeline;
}