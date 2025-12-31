package com.unlimited.sports.globox.social.service.impl;

import com.unlimited.sports.globox.model.social.entity.ConversationUpdateEvent;
import com.unlimited.sports.globox.model.social.entity.MessageQueueEvent;
import com.unlimited.sports.globox.social.config.RabbitMQDeadLetterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 死信消息消费者服务
 * 负责处理失败的消息，进行监控和告警
 * TODO ETA 2026.01.03 死信队列消费
 */
@Slf4j
@Service
public class DeadLetterConsumerService {

    @Autowired
    private MessageProducerService messageProducerService;

    /**
     * 消费消息死信队列
     */
    @RabbitListener(queues = RabbitMQDeadLetterConfig.MESSAGE_DLQ)
    public void consumeMessageDeadLetter(MessageQueueEvent event) {
        log.error("消息进入死信队列，messageId: {}, fromUserId: {}, toUserId: {}, 重试次数: {}", 
            event.getMessageId(), event.getFromUserId(), event.getToUserId(), event.getRetryCount());
        // 如果重试次数还未达到上限，可以尝试重新投递

        if (event.getRetryCount() != null && event.getRetryCount() < 5) {
            log.info("死信消息准备重新投递，messageId: {}", event.getMessageId());
            messageProducerService.sendRetryMessage(event, 10000); // 10秒后重试
        } else {
            log.error("死信消息已达到最大重试次数，messageId: {}", event.getMessageId());
            // 记录到失败表或发送告警通知
        }
    }

    /**
     * 消费会话死信队列
     */
    @RabbitListener(queues = RabbitMQDeadLetterConfig.CONVERSATION_DLQ)
    public void consumeConversationDeadLetter(ConversationUpdateEvent event) {
        log.error("会话更新进入死信队列，conversationId: {}, operationType: {}", 
            event.getConversationId(), event.getOperationType());

    }

    /**
     * 消费批量消息死信队列
     */
    @RabbitListener(queues = RabbitMQDeadLetterConfig.BATCH_MESSAGE_DLQ)
    public void consumeBatchMessageDeadLetter(MessageQueueEvent event) {
        log.error("批量消息进入死信队列，messageId: {}, fromUserId: {}, toUserId: {}", 
            event.getMessageId(), event.getFromUserId(), event.getToUserId());
        
        // 批量消息失败不影响其他消息，记录日志即可
    }
}
