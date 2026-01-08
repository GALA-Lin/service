package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;



/**
 * 额外费用退款进度 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RefundExtraChargeProgressVo", description = "额外费用退款进度返回对象")
public class RefundExtraChargeProgressVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Schema(description = "额外费用记录ID", example = "50001")
    private Long extraChargeId;

    /**
     * 订单项ID：订单级额外费用为空
     */
    @Null
    @Schema(description = "订单项ID（订单级额外费用时为空或为0）",
            example = "10001",
            nullable = true)
    private Long orderItemId;

    /**
     * 该额外费用本次退款金额（快照）
     */
    @NotNull
    @Schema(description = "该额外费用本次退款金额", example = "20.00")
    private BigDecimal refundAmount;

    @Null
    @Schema(description = "费用名称（快照）",
            example = "夜场附加费",
            nullable = true)
    private String chargeName;

    /**
     * 退款状态
     */
    @NotNull
    @Schema(description = "退款状态", example = "REFUNDING")
    private RefundStatusEnum refundStatus;

    @NotNull
    @Schema(description = "退款状态描述", example = "退款处理中")
    private String refundStatusName;
}