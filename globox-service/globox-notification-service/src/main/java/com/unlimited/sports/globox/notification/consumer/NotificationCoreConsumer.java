package com.unlimited.sports.globox.notification.consumer;

import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import com.unlimited.sports.globox.model.notification.dto.NotificationMessage;
import com.unlimited.sports.globox.notification.service.INotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 核心业务通知消费者
 * 处理订单、预约、支付、社交相关通知
 *
 * 重试策略：Spring AMQP自动重试3次，失败后进死信队列
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
    public void onMessage(NotificationMessage message) throws Exception {
        String messageId = message.getMessageId();
        String messageType = message.getMessageType();


        log.info("[核心业务通知] messageId={}, messageType={}, traceId={}",
                messageId, messageType, message.getTraceId());
        try {
            notificationService.handleNotification(message);

        } catch (Exception e) {
            log.error("[核心业务通知]处理失败 messageId={}, messageType={}, 异常: {}",
                    messageId, messageType, e.getMessage());
            throw e;
        }
    }
}