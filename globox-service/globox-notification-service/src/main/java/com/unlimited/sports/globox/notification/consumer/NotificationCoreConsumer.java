package com.unlimited.sports.globox.notification.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.message.notification.NotificationMessage;
import com.unlimited.sports.globox.notification.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 核心业务通知消费者
 * 处理订单、预约、支付、社交相关通知
 *
 * 重试策略：最大5次，重试间隔3秒
 */
@Slf4j
@Component
@RabbitListener(queues = NotificationMQConstants.QUEUE_NOTIFICATION_CORE, concurrency = "5-10")
public class NotificationCoreConsumer {

    private final INotificationService notificationService;

    public NotificationCoreConsumer(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            maxRetryCount = 5,
            finalExchange = NotificationMQConstants.EXCHANGE_NOTIFICATION_CORE_FINAL_DLX,
            finalRoutingKey = NotificationMQConstants.ROUTING_NOTIFICATION_CORE_FINAL,
            bizKey = "#message.messageId",
            bizType = MQBizTypeEnum.NOTIFICATION_CORE
    )
    public void onMessage(NotificationMessage message, Channel channel, Message amqpMessage) throws Exception {
        String messageId = message.getMessageId();
        String messageType = message.getMessageType();

        log.info("[核心业务通知] messageId={}, messageType={}, traceId={}",
                messageId, messageType, message.getTraceId());

        notificationService.handleNotification(message);
    }
}