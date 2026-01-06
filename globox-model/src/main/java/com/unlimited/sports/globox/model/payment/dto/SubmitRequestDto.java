package com.unlimited.sports.globox.model.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 下单请求 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitRequestDto {

    private Long orderNo;

    private Integer paymentTypeCode;

    private Integer clientTypeCode;

    /**
     * 支付时必须
     * 只有满足以下条件时该字段必须传入：
     *  1. 微信支付
     *  2. 小程序端
     */
    private String openId;
}
