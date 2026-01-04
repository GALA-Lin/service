package com.unlimited.sports.globox.common.message.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单退款 message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRefundMessage {

    private Long orderNo;

    /**
     * 第三方退款 NO
     */
    private String outRequestNo;
}
