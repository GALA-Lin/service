package com.unlimited.sports.globox.model.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "GetRefundProgressRequestDto", description = "查询退款进度请求参数")
public class GetRefundProgressRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     * refundApplyId 为空时必填，用于定位该订单的“最新退款申请”
     */
    @Schema(description = "订单号（refundApplyId 为空时必填，用于查询该订单的最新退款申请）",
            example = "202512180001",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long orderNo;

    /**
     * 退款申请ID
     * 若传入则按该申请查询
     */
    @Schema(description = "退款申请ID（优先级高于 orderNo）",
            example = "10086",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long refundApplyId;

    /**
     * 参数校验：orderNo 与 refundApplyId 二选一，不能同时为空
     */
    @NotNull(message = "orderNo 与 refundApplyId 不能同时为空")
    @Schema(hidden = true)
    public Long getEitherOrderNoOrRefundApplyId() {
        return (refundApplyId != null) ? refundApplyId : orderNo;
    }
}