package com.unlimited.sports.globox.notification.consumer;

import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import com.unlimited.sports.globox.model.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 死信队列消费者
 * 处理所有处理失败且超过重试次数的消息
 *
 * 消息进入这个队列意味着：
 * - 在原队列中处理失败
 * - 自动重试3次全部失败
 * - 消息最终被路由到死信队列
 */
@Slf4j
@Component
@RabbitListener(queues = NotificationMQConstants.QUEUE_DLQ_NOTIFICATION)
public class NotificationDlqConsumer {

    @RabbitHandler
    public void onMessage(NotificationMessage message) {
        String messageId = message.getMessageId();
        String messageType = message.getMessageType();
        log.error("死信队列收到消息，{}",
               message); // todo 目前直接日志,后续管理平台需要获取到这些消费失败的消息
    }
}