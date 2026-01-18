package com.unlimited.sports.globox.order.service;

import com.unlimited.sports.globox.common.enums.order.OperatorTypeEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.message.payment.PaymentRefundMessage;

import java.math.BigDecimal;

/**
 * 订单退款实际动作执行
 */
public interface OrderRefundActionService {
    /**
     * 退款动作执行方法
     *
     * @param orderNo       订单编号
     * @param refundApplyId 退款单号
     * @param isAutoRefund  商家是否自动确认
     * @param operatorId    商家 id
     * @param operatorType  操作人类型
     * @return 退款涉及到的总订单项数
     */
    int refundAction(Long orderNo,
            Long refundApplyId,
            boolean isAutoRefund,
            Long operatorId,
            OperatorTypeEnum operatorType,
            SellerTypeEnum sellerType);

    void refundSuccess(PaymentRefundMessage message);
}
