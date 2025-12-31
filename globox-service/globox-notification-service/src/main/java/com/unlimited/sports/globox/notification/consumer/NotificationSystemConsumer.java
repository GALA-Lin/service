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
 * 系统消息通知消费者
 * 处理系统公告、营销推送等低优先级消息
 *
 * 重试策略：Spring AMQP自动重试3次，失败后进死信队列
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
    public void onMessage(NotificationMessage message) throws Exception {
        String messageId = message.getMessageId();
        String messageType = message.getMessageType();
        log.info("[系统消息通知] messageId={}, messageType={}, traceId={}",
                messageId, messageType, message.getTraceId());
        try {
            notificationService.handleNotification(message);
        } catch (Exception e) {
            log.error("[系统消息通知] ✗ 处理失败 messageId={}, messageType={}, 异常: {}",
                    messageId, messageType, e.getMessage());
            throw e;
        }
    }
}
