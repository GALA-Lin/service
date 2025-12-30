package com.unlimited.sports.globox.model.order.vo;

import com.unlimited.sports.globox.common.enums.order.RefundStatusEnum;
import lombok.*;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundExtraChargeProgressVo implements Serializable {

    @NotNull
    private Long extraChargeId;

    /**
     * 订单项ID：订单级额外费用为空
     */
    @NotNull
    private Long orderItemId;

    /**
     * 该额外费用本次退款金额（快照）
     */
    @NotNull
    private BigDecimal refundAmount;

    @Null
    private String chargeName;

    /**
     * 退款状态
     */
    @NotNull
    private RefundStatusEnum refundStatus;
    @NotNull
    private String refundStatusName;
}