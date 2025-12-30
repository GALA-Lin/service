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
public class RefundItemProgressVo implements Serializable {

    @NotNull
    private Long orderItemId;

    @NotNull
    private String resourceName;

    /**
     * order_items.refund_status
     */
    @NotNull
    private RefundStatusEnum refundStatus;
    @NotNull
    private String refundStatusName;

    /**
     * 退款事实（order_item_refunds），可能为空（例如刚申请、尚未审批生成事实）
     */
    @Null
    private BigDecimal itemAmount;
    /**
     * 这里只做“聚合展示”，明细以 extraCharges 为准
     */
    @Null
    private BigDecimal extraChargeAmount;
    @Null
    private BigDecimal refundFee;
    @Null
    private BigDecimal refundAmount;
}