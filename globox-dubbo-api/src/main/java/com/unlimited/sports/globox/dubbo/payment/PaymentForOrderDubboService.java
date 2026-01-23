package com.unlimited.sports.globox.dubbo.payment;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.payment.dto.UserRefundRequestDto;

/**
 * 支付服务对订单 service
 */
public interface PaymentForOrderDubboService {
    RpcResult<Void> userRefund(UserRefundRequestDto dto);
}
