package com.unlimited.sports.globox.merchant.customer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.message.order.OrderNotifyMerchantConfirmMessage;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.MerchantConfirmRequestDto;
import com.unlimited.sports.globox.dubbo.order.dto.SellerConfirmResultDto;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @since 2026/1/3 18:15
 * 商家确认消费者
 */
@Slf4j
@Component
public class MerchantOrderConfirm {

    private final VenueMapper venueMapper;

    public MerchantOrderConfirm(VenueMapper venueMapper) {
        this.venueMapper = venueMapper;
    }


    @DubboReference(group = "rpc")
    private OrderForMerchantDubboService orderDubboService;



    @RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_CONFIRM_NOTIFY_MERCHANT)
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_CONFIRM_NOTIFY_MERCHANT_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_CONFIRM_NOTIFY_MERCHANT_FINAL
    )
    public void onMessage(
            OrderNotifyMerchantConfirmMessage message,
            Channel channel,
            Message amqpMessage) {
        Long orderNo = message.getOrderNo();
        log.info("[商家自动确认消费] 收到支付成功消息, 订单号: {}", orderNo);
        // 1. 构建请求参数
        MerchantConfirmRequestDto requestDto = MerchantConfirmRequestDto.builder()
                .orderNo(orderNo)
                .isAutoConfirm(true) // 当前业务：全自动确认
                // 注意：可以从 message 或先查一遍订单获取真正的 merchantId
                .merchantId(getMerchantId(message.getVenueId()))
                .build();
        // 2. 调用远程服务执行确认逻辑
        RpcResult<SellerConfirmResultDto> result = orderDubboService.confirm(requestDto);
//      SellerConfirmResultDto result = orderDubboService.confirm(requestDto).getData();
        Assert.rpcResultOk(result);
        if (result.isSuccess()) {
            log.info("[商家自动确认消费] 订单 {} 确认成功, 当前状态: {}", orderNo, result.getData().getOrderStatusName());
        } else {
            // 如果是业务预期的“无法确认”（例如订单已过期），记录 warn 即可，不触发重试
            log.warn("[商家自动确认消费] 订单 {} 确认失败, 原因: {}", orderNo, result.getData().getOrderStatusName());
        }
    }

    /**
     * 通过 venueId 查询 merchantId
     */
    Long getMerchantId(Long venueId){
        return venueMapper.selectMerchantIdByVenueId(venueId);
    }
}
