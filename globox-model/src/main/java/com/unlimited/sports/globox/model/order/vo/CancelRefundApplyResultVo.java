package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.time.LocalDateTime;


/**
 * 代表取消退款申请后的结果值对象。
 * 包含了订单编号、退款申请ID、申请单状态及其名称、订单状态及其名称、取消时间以及可选的提示信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "CancelRefundApplyResultVo",
        description = "取消退款申请结果返回对象"
)
public class CancelRefundApplyResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @NotNull
    @Schema(description = "订单号", example = "202512180001")
    private Long orderNo;

    /**
     * 退款申请ID
     */
    @NotNull
    @Schema(description = "退款申请ID", example = "10086")
    private Long refundApplyId;

    /**
     * 退款申请状态
     */
    @NotNull
    @Schema(description = "退款申请状态", example = "CANCELLED")
    private ApplyRefundStatusEnum applyStatus;

    /**
     * 退款申请状态描述
     */
    @NotNull
    @Schema(description = "退款申请状态描述", example = "已取消")
    private String applyStatusName;

    /**
     * 订单状态
     * 取消退款后订单状态可能回退或调整
     */
    @NotNull
    @Schema(description = "订单状态", example = "PAID")
    private OrderStatusEnum orderStatus;

    /**
     * 订单状态描述
     */
    @NotNull
    @Schema(description = "订单状态描述", example = "已支付")
    private String orderStatusName;

    /**
     * 退款申请取消时间
     */
    @Null
    @Schema(description = "退款申请取消时间（若未实际取消则为空）",
            example = "2025-12-18T17:20:00",
            nullable = true)
    private LocalDateTime cancelledAt;
}