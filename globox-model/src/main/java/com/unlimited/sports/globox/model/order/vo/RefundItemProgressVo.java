package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单项退款进度 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "RefundItemProgressVo",
        description = "订单项退款进度返回对象"
)
public class RefundItemProgressVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "订单项ID", example = "10001")
    private Long orderItemId;

    @NotNull
    @Schema(description = "资源名称（场地名 / 教练名）", example = "1号场")
    private String resourceName;

    /**
     * order_items.refund_status
     */
    @NotNull
    @Schema(description = "订单项退款状态", example = "REFUNDING")
    private RefundStatusEnum refundStatus;

    @NotNull
    @Schema(description = "订单项退款状态描述", example = "退款处理中")
    private String refundStatusName;

    /**
     * 退款事实（order_item_refunds）
     * 可能为空（例如刚申请、尚未审批生成事实）
     */
    @Null
    @Schema(description = "订单项退款金额（基础金额快照）",
            example = "120.00",
            nullable = true)
    private BigDecimal itemAmount;

    /**
     * 这里只做“聚合展示”，明细以 extraCharges 为准
     */
    @Null
    @Schema(description = "订单项额外费用退款金额汇总",
            example = "20.00",
            nullable = true)
    private BigDecimal extraChargeAmount;

    @Null
    @Schema(description = "退款手续费",
            example = "5.00",
            nullable = true)
    private BigDecimal refundFee;

    @Null
    @Schema(description = "订单项最终退款金额（itemAmount + extraChargeAmount - refundFee）",
            example = "135.00",
            nullable = true)
    private BigDecimal refundAmount;
}