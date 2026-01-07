package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 商家同意退款结果的数据传输对象。
 * 该类用于封装商家对退款申请的处理结果，包括订单状态、退款申请状态等信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantApproveRefundResultDto implements Serializable {

    @NotNull
    private Long orderNo;

    @NotNull
    private Long refundApplyId;

    @NotNull
    private ApplyRefundStatusEnum applyStatus;
    @NotNull
    private LocalDateTime reviewedAt;

    @NotNull
    private OrderStatusEnum orderStatus;

    @NotNull
    private String orderStatusName;

    @NotNull
    private BigDecimal refundPercentage;

    /**
     * 本次同意退款的 item 数量
     */
    @NotNull
    private Integer approvedItemCount;
}