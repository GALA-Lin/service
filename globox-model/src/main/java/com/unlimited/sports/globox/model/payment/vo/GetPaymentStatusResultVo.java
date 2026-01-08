package com.unlimited.sports.globox.model.payment.vo;

import com.unlimited.sports.globox.common.enums.payment.PaymentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 获取支付状态 vo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPaymentStatusResultVo implements Serializable {

    private String outTradeNo;

    private PaymentStatusEnum paymentStatus;
}
