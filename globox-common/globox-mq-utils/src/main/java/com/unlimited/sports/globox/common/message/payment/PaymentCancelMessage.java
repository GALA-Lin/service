package com.unlimited.sports.globox.common.message.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付取消消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelMessage {

    private Long orderNo;

}
