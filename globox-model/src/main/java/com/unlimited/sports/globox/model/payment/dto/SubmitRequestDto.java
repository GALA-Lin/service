package com.unlimited.sports.globox.model.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 下单请求 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequestDto {

    private Long orderNo;

    @NotNull(message = "支付类型不能为空")
    @Schema(description = "支付类型：1=微信支付。2=支付宝支付",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer paymentTypeCode;

    @NotNull(message = "客户端类型不能为空")
    @Schema(description = "客户端类型：1=APP，2=微信小程序",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer clientTypeCode;

    /**
     * 支付时必须
     * 只有满足以下条件时该字段必须传入：
     *  1. 微信支付
     *  2. 小程序端
     */
    @Schema(description = "支付者微信 openId ，支付时必须只有满足以下条件时该字段必须传入：1. 微信支付2. 小程序端",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String openId;
}
