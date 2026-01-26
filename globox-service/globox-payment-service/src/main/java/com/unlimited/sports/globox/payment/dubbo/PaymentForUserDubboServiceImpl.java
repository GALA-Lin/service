package com.unlimited.sports.globox.payment.dubbo;

import com.unlimited.sports.globox.common.result.PaymentsCode;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.payment.PaymentForUserDubboService;
import com.unlimited.sports.globox.dubbo.payment.dto.RegisterProfitSharingCoachReceiverDto;
import com.unlimited.sports.globox.payment.prop.WechatPayProperties;
import com.wechat.pay.java.service.profitsharing.ProfitsharingService;
import com.wechat.pay.java.service.profitsharing.model.AddReceiverRequest;
import com.wechat.pay.java.service.profitsharing.model.AddReceiverResponse;
import com.wechat.pay.java.service.profitsharing.model.ReceiverRelationType;
import com.wechat.pay.java.service.profitsharing.model.ReceiverType;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 支付服务向用户服务提供的 dubbo service
 */
@Slf4j
@Component
@DubboService(group = "rpc")
public class PaymentForUserDubboServiceImpl implements PaymentForUserDubboService {

    @Autowired
    private ProfitsharingService profitsharingService;

    @Autowired
    private WechatPayProperties wechatPayProperties;


    @Override
    public RpcResult<Void> registerProfitSharingCoachReceiver(RegisterProfitSharingCoachReceiverDto dto) {
        try {
            AddReceiverRequest receiverRequest = new AddReceiverRequest();
            receiverRequest.setAccount(dto.getAccount());
            receiverRequest.setAppid(wechatPayProperties.getAppid());
            receiverRequest.setType(ReceiverType.PERSONAL_OPENID);
            receiverRequest.setRelationType(ReceiverRelationType.PARTNER);
            profitsharingService.addReceiver(receiverRequest);
            return RpcResult.ok();
        } catch (Exception e) {
            log.error("registerProfitSharingCoachReceiver error", e);
            return RpcResult.error(PaymentsCode.REGISTER_PROFIT_SHARING_RECEIVER_FAILED);
        }
    }
}
