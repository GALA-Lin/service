package com.unlimited.sports.globox.model.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "CancelRefundApplyRequestDto", description = "用户取消退款申请请求参数")
public class CancelRefundApplyRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @NotNull(message = "订单号不能为空")
    @Schema(description = "订单号",
            example = "202512180001",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderNo;

    /**
     * 退款申请ID
     */
    @NotNull(message = "退款申请ID不能为空")
    @Schema(description = "退款申请ID",
            example = "10086",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long refundApplyId;
}