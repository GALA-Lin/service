package com.unlimited.sports.globox.venue.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.message.order.OrderPaidMessage;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.utils.Assert;
import com.unlimited.sports.globox.dubbo.order.OrderForMerchantDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.MerchantConfirmRequestDto;
import com.unlimited.sports.globox.dubbo.order.dto.SellerConfirmResultDto;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.venue.service.IActivityReminderService;
import com.unlimited.sports.globox.venue.service.IVenueBookingReminderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 场馆订单支付成功消费者
 * 接收订单服务的支付成功通知，发送订场/活动提醒延迟消息
 */
@Slf4j
@Component
@RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT, concurrency = "5-10")
public class VenuePaymentSuccessConsumer {

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private IVenueBookingReminderService venueBookingReminderService;

    @Autowired
    private IActivityReminderService activityReminderService;

    @DubboReference(group = "rpc")
    private OrderForMerchantDubboService orderDubboService;

    /**
     * 处理场馆订单支付成功消息
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_PAYMENT_CONFIRMED_NOTIFY_MERCHANT_FINAL
    )
    public void onMessage(OrderPaidMessage message, Channel channel, Message amqpMessage) {
        Long userId = message.getUserId();
        List<Long> recordIds = message.getRecordIds();
        Boolean isActivity = message.getIsActivity();
        Long orderNo = message.getOrderNo();

        log.info("[场馆支付成功] 接收到消息 - userId={}, recordIds={}, isActivity={}",
                userId, recordIds, isActivity);

        // 校验参数
        if (CollectionUtils.isEmpty(recordIds)) {
            log.warn("[场馆支付成功] ID列表为空 - userId={}", userId);
            return;
        }

        try {
            // 根据 isActivity 判断是普通槽位还是活动槽位
            if (Boolean.TRUE.equals(isActivity)) {
                // 活动订单，recordIds.get(0) 是活动ID
                Long activityId = recordIds.get(0);
                activityReminderService.sendActivityReminderMessage(userId, activityId);
                log.info("[场馆支付成功] 活动提醒延迟消息已发送 - userId={}, activityId={}",
                        userId, activityId);
            } else {
                // 普通订场订单，recordIds 是槽位记录ID列表
                venueBookingReminderService.sendBookingReminderMessages(userId, recordIds);
                log.info("[场馆支付成功] 订场提醒延迟消息已发送 - userId={}, recordIds={}",
                        userId, recordIds);
            }

            // 1. 构建请求参数
            MerchantConfirmRequestDto requestDto = MerchantConfirmRequestDto.builder()
                    .orderNo(orderNo)
                    .isAutoConfirm(true) // 当前业务：全自动确认
                    // 注意：可以从 message 或先查一遍订单获取真正的 merchantId
                    .merchantId(getMerchantId(message.getVenueId()))
                    .build();
            // 2. 调用远程服务执行确认逻辑
            RpcResult<SellerConfirmResultDto> result = orderDubboService.confirm(requestDto);
            Assert.rpcResultOk(result);

            if (result.isSuccess()) {
                log.info("[商家自动确认消费] 订单 {} 确认成功, 当前状态: {}", orderNo, result.getData().getOrderStatusName());
            } else {
                // 如果是业务预期的“无法确认”（例如订单已过期），记录 warn 即可，不触发重试
                log.warn("[商家自动确认消费] 订单 {} 确认失败, 原因: {}", orderNo, result.getData().getOrderStatusName());
            }

        } catch (Exception e) {
            log.error("[场馆支付成功] 处理失败 - userId={}, recordIds={}, isActivity={}",
                    userId, recordIds, isActivity, e);
            throw e; // 重新抛出以触发重试机制
        }
    }



    /**
     * 通过 venueId 查询 merchantId
     */
    Long getMerchantId(Long venueId){
        return venueMapper.selectMerchantIdByVenueId(venueId);
    }
}
