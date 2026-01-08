package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.ApplyRefundStatusEnum;
import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 商家退款 结果 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerRefundResultDto implements Serializable {

    @NotNull
    private Long orderNo;

    @NotNull
    private Long refundApplyId;

    @NotNull
    private ApplyRefundStatusEnum applyStatus;

    @NotNull
    private OrderStatusEnum orderStatus;

    @NotNull
    private String orderStatusName;
}
