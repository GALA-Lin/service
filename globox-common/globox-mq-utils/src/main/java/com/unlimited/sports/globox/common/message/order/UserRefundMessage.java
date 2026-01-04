package com.unlimited.sports.globox.common.message.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 用户退款消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRefundMessage {

    private Long orderNo;
    private String outTradeNo;
    private String outRequestNo;
    private BigDecimal refundAmount;
    private String refundReason;

}
