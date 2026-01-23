package com.unlimited.sports.globox.payment.dubbo;

import com.unlimited.sports.globox.common.result.ApplicationCode;
import com.unlimited.sports.globox.common.result.ResultCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.payment.PaymentForOrderDubboService;
import com.unlimited.sports.globox.dubbo.payment.dto.UserRefundRequestDto;
import com.unlimited.sports.globox.payment.service.PaymentsService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付向订单提供的 rpc 接口
 */
@Slf4j
@Component
@DubboService(group = "rpc")
public class PaymentForOrderDubboServiceImpl implements PaymentForOrderDubboService {

    @Autowired
    private PaymentsService paymentsService;

    @Override
    @GlobalTransactional
    public RpcResult<Void> userRefund(UserRefundRequestDto dto) {
        ResultCode code = paymentsService.refund(dto);
        if (ApplicationCode.SUCCESS.equals(code)) {
            return RpcResult.ok();
        } else {
            return RpcResult.error(code);
        }
    }
}
