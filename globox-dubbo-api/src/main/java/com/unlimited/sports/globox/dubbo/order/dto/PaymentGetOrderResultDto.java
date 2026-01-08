package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 支付模块调用订单模块获取订单详情 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentGetOrderResultDto implements Serializable {
    private Long orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 交易内容
     */
    private String subject;

    /**
     * 支付金额
     */
    private BigDecimal totalAmount;

    /**
     * 是否是活动订单
     */
    private boolean isActivity;
}
