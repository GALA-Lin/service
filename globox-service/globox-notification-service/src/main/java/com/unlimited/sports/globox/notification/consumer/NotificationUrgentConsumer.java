package com.unlimited.sports.globox.notification.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import com.unlimited.sports.globox.common.message.notification.NotificationMessage;
import com.unlimited.sports.globox.notification.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 紧急业务通知消费者
 * 处理账户被锁定、异常告警等紧急通知
 *
 * 重试策略：最大5次，重试间隔1秒
 */
@Slf4j
@Component
@RabbitListener(queues = NotificationMQConstants.QUEUE_NOTIFICATION_URGENT, concurrency = "5-10")
public class NotificationUrgentConsumer {

    private final INotificationService notificationService;

    public NotificationUrgentConsumer(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            maxRetryCount = 5,
            finalExchange = NotificationMQConstants.EXCHANGE_NOTIFICATION_URGENT_FINAL_DLX,
            finalRoutingKey = NotificationMQConstants.ROUTING_NOTIFICATION_URGENT_FINAL
    )
    public void onMessage(NotificationMessage message, Channel channel, Message amqpMessage) throws Exception {

        String messageId = message.getMessageId();
        String messageType = message.getMessageType();
        log.warn("[紧急业务通知] messageId={}, messageType={}, traceId={}",
                messageId, messageType, message.getTraceId());

        notificationService.handleNotification(message);
    }
}