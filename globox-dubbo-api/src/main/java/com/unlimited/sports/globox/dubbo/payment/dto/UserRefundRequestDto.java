package com.unlimited.sports.globox.dubbo.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRefundRequestDto implements Serializable {

    /**
     * 订单编号
     */
    private Long orderNo;

    /**
     * 第三方订单 id
     */
    private String outTradeNo;

    /**
     * 本次退款 id
     */
    private String outRequestNo;

    /**
     * 本次退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 是否因订单被取消，由系统发起的退款
     */
    private boolean orderCancelled = false;

    /**
     * 是否整单退款
     */
    private boolean fullRefund;

}