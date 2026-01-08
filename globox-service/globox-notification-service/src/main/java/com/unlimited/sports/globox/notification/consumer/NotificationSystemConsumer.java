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
 * 系统消息通知消费者
 * 处理系统公告、营销推送等低优先级消息
 *
 * 重试策略：最大3次，重试间隔5秒
 */
@Slf4j
@Component
@RabbitListener(queues = NotificationMQConstants.QUEUE_NOTIFICATION_SYSTEM, concurrency = "5-10")
public class NotificationSystemConsumer {

    private final INotificationService notificationService;

    public NotificationSystemConsumer(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            maxRetryCount = 3,
            finalExchange = NotificationMQConstants.EXCHANGE_NOTIFICATION_SYSTEM_FINAL_DLX,
            finalRoutingKey = NotificationMQConstants.ROUTING_NOTIFICATION_SYSTEM_FINAL
    )
    public void onMessage(NotificationMessage message, Channel channel, Message amqpMessage) throws Exception {

        String messageId = message.getMessageId();
        String messageType = message.getMessageType();
        log.info("[系统消息通知] messageId={}, messageType={}, traceId={}",
                messageId, messageType, message.getTraceId());

        notificationService.handleNotification(message);
    }
}
