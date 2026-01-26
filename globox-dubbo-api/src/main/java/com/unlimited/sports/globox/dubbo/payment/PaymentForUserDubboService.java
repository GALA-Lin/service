package com.unlimited.sports.globox.dubbo.payment;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.payment.dto.RegisterProfitSharingCoachReceiverDto;

/**
 * 支付服务向用户服务提供的 dubbo service
 */
public interface PaymentForUserDubboService {

    /**
     * 注册分账方
     */
    RpcResult<Void> registerProfitSharingCoachReceiver(RegisterProfitSharingCoachReceiverDto dto);
}
