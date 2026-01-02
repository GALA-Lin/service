package com.unlimited.sports.globox.common.message.payment;

import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付成功消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessMessage implements Serializable {

    private Long orderNo;

    /**
     * 第三方交易编号，由第三方平台提供
     */
    private String tradeNo;

    /**
     * 支付类型（1=WECHAT / 2=ALIPAY）
     */
    private PaymentTypeEnum paymentType;

    /**
     * 支付金额
     */
    private BigDecimal totalAmount;

    /**
     * 用户支付时间
     */
    private LocalDateTime paymentAt;
}
