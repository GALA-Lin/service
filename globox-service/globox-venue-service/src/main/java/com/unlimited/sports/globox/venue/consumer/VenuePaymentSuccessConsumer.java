package com.unlimited.sports.globox.venue.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.constants.PaymentMQConstants;
import com.unlimited.sports.globox.common.message.order.OrderPaidMessage;
import com.unlimited.sports.globox.venue.service.IActivityReminderService;
import com.unlimited.sports.globox.venue.service.IVenueBookingReminderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.CollectionUtils;
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
    private IVenueBookingReminderService venueBookingReminderService;

    @Autowired
    private IActivityReminderService activityReminderService;

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

        } catch (Exception e) {
            log.error("[场馆支付成功] 处理失败 - userId={}, recordIds={}, isActivity={}",
                    userId, recordIds, isActivity, e);
            throw e; // 重新抛出以触发重试机制
        }
    }
}
