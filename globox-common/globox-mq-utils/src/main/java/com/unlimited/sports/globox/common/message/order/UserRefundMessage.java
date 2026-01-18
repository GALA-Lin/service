package com.unlimited.sports.globox.common.message.order;

import com.unlimited.sports.globox.common.enums.ClientType;
import com.unlimited.sports.globox.common.enums.order.PaymentTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 用户退款消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRefundMessage implements Serializable {

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
