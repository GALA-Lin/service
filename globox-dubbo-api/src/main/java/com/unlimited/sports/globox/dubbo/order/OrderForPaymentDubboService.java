package com.unlimited.sports.globox.dubbo.order;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.order.dto.PaymentGetOrderResultDto;

/**
 * 订单服务对支付服务提供 rpc 接口
 */
public interface OrderForPaymentDubboService {



    /**
     * payment 支付前查询订单情况
     *
     * @param orderNo 订单编号
     * @return 订单相关信息
     */
    RpcResult<PaymentGetOrderResultDto> paymentGetOrders(Long orderNo);
}
