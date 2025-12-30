package com.unlimited.sports.globox.model.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用于表示用户取消退款申请时所需的数据传输对象。
 * 此DTO包含订单号、退款申请ID以及可选的取消原因，用于处理用户的退款取消请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelRefundApplyRequestDto implements Serializable {

    /**
     * 订单号
     */
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    /**
     * 退款申请ID
     */
    @NotNull(message = "退款申请ID不能为空")
    private Long refundApplyId;
}