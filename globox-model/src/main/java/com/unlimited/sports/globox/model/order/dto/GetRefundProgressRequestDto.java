package com.unlimited.sports.globox.model.order.dto;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetRefundProgressRequestDto implements Serializable {

    /**
     * 订单号：refundApplyId 为空时必填，用于定位该订单的“最新退款申请”
     */
    private Long orderNo;

    /**
     * 退款申请ID：若传入则按该申请查询
     */
    private Long refundApplyId;

    /**
     * 是否返回时间线（状态日志）
     */
    @Builder.Default
    private Boolean includeTimeline = true;

    /**
     * 参数校验：二选一
     */
    @NotNull(message = "orderNo 与 refundApplyId 不能同时为空")
    public Long getEitherOrderNoOrRefundApplyId() {
        return (refundApplyId != null) ? refundApplyId : orderNo;
    }
}