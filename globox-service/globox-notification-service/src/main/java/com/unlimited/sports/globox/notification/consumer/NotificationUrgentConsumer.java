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
 * 紧急业务通知消费者
 * 处理账户被锁定、异常告警等紧急通知
 *
 * 重试策略：Spring AMQP自动重试3次，失败后进死信队列
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
    public void onMessage(NotificationMessage message) throws Exception {
        String messageId = message.getMessageId();
        String messageType = message.getMessageType();
        log.warn("[紧急业务通知] messageId={}, messageType={}, traceId={}",
                messageId, messageType, message.getTraceId());
        try {
            notificationService.handleNotification(message);
        } catch (Exception e) {
            log.error("[紧急业务通知] ✗ 处理失败 messageId={}, messageType={}, 异常: {}",
                    messageId, messageType, e.getMessage());
            throw e;
        }
    }
}