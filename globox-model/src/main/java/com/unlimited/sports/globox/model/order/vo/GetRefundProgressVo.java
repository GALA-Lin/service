package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import com.unlimited.sports.globox.common.enums.order.UserRefundReasonEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款进度详情返回 VO
 *
 * 用于退款进度页，聚合订单状态、退款申请状态、金额汇总、
 * 订单项退款进度、额外费用退款以及时间线
 *
 * @author beanmak1r
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "GetRefundProgressVo",
        description = "退款进度详情返回对象"
)
public class GetRefundProgressVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "订单号", example = "202512180001")
    private Long orderNo;

    @NotNull
    @Schema(description = "退款申请ID", example = "10086")
    private Long refundApplyId;

    /**
     * 订单状态
     */
    @NotNull
    @Schema(description = "订单当前状态", example = "PAID")
    private OrderStatusEnum orderStatus;

    @NotNull
    @Schema(description = "订单状态描述", example = "已支付")
    private String orderStatusName;

    /**
     * 申请单状态：PENDING / APPROVED / REJECTED / REFUNDING / REFUNDED
     */
    @NotNull
    @Schema(description = "退款申请状态", example = "APPROVED")
    private ApplyRefundStatusEnum applyStatus;

    @NotNull
    @Schema(description = "退款申请状态描述", example = "审核通过")
    private String applyStatusName;

    /**
     * 申请信息
     */
    @NotNull
    @Schema(description = "退款原因编码", example = "QUALITY_ISSUE")
    private UserRefundReasonEnum reasonCode;

    @Null
    @Schema(
            description = "退款原因补充说明",
            example = "场地条件与描述不符",
            nullable = true
    )
    private String reasonDetail;

    @Null
    @Schema(
            description = "退款申请时间",
            example = "2025-12-18T14:30:00",
            nullable = true
    )
    private LocalDateTime appliedAt;

    @Null
    @Schema(
            description = "审核时间",
            example = "2025-12-18T15:10:00",
            nullable = true
    )
    private LocalDateTime reviewedAt;

    @Null
    @Schema(
            description = "商家审核备注",
            example = "符合退款条件，已通过",
            nullable = true
    )
    private String sellerRemark;

    /**
     * 支付平台退款过程
     */
    @Null
    @Schema(
            description = "退款发起时间（支付平台）",
            example = "2025-12-18T15:20:00",
            nullable = true
    )
    private LocalDateTime refundInitiatedAt;

    @Null
    @Schema(
            description = "退款完成时间（支付平台）",
            example = "2025-12-18T15:45:00",
            nullable = true
    )
    private LocalDateTime refundCompletedAt;

    @Null
    @Schema(
            description = "支付平台退款流水号",
            example = "WX2025121815450001",
            nullable = true
    )
    private String refundTransactionId;

    /**
     * 汇总金额
     * 申请 / 审批口径的应退总额（含 item + extra）
     */
    @NotNull
    @Schema(description = "应退总金额", example = "260.00")
    private BigDecimal totalRefundAmount;

    /**
     * 已完成退款总额
     */
    @NotNull
    @Schema(description = "已完成退款金额", example = "200.00")
    private BigDecimal refundedAmount;

    /**
     * 处理中金额
     */
    @NotNull
    @Schema(description = "退款处理中金额", example = "60.00")
    private BigDecimal refundingAmount;

    /**
     * 本次申请涉及的订单项进度
     */
    @NotNull
    @Schema(description = "订单项退款进度列表")
    private List<RefundItemProgressVo> items;

    /**
     * 本次申请涉及的额外费用退款明细
     * （item级 + 订单级；来自 order_refund_extra_charges）
     */
    @Null
    @Schema(
            description = "额外费用退款进度明细",
            nullable = true
    )
    private List<RefundExtraChargeProgressVo> extraCharges;
}