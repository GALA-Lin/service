package com.unlimited.sports.globox.social.service.impl;

import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.model.social.entity.ConversationUpdateEvent;
import com.unlimited.sports.globox.model.social.entity.MessageQueueEvent;
import com.unlimited.sports.globox.social.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息生产者服务
 * 负责将消息发送到RabbitMQ队列
 * 使用 globox-mq-utils 工具类实现消息发送、重试和监控
 */
@Slf4j
@Service
public class MessageProducerService {

    @Autowired
    private MQService mqService;

    /**
     * 发送单条消息到队列
     * 使用 globox-mq-utils 工具类，具备自动重试和监控功能
     */
    public void sendMessageToQueue(MessageQueueEvent event) {
        try {
            event.setCreatedAt(LocalDateTime.now());
            event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() : 0);
            
            boolean success = mqService.send(
                RabbitMQConfig.SOCIAL_EXCHANGE,
                RabbitMQConfig.MESSAGE_ROUTING_KEY,
                event
            );
            
            if (success) {
                log.info("消息已发送到队列，messageId: {}, fromUserId: {}, toUserId: {}", 
                    event.getMessageId(), event.getFromUserId(), event.getToUserId());
            } else {
                log.error("消息发送失败，messageId: {}", event.getMessageId());
                throw new RuntimeException("消息发送失败");
            }
        } catch (Exception e) {
            log.error("发送消息到队列失败", e);
            throw new RuntimeException("发送消息到队列失败: " + e.getMessage());
        }
    }

    /**
     * 发送批量消息到队列
     * 使用 globox-mq-utils 工具类，每条消息都有独立的重试机制
     */
    public void sendBatchMessageToQueue(List<MessageQueueEvent> events) {
        try {
            int successCount = 0;
            for (MessageQueueEvent event : events) {
                event.setCreatedAt(LocalDateTime.now());
                event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() : 0);
                
                // 使用 MQUtils 工具类发送消息
                boolean success = mqService.send(
                    RabbitMQConfig.SOCIAL_EXCHANGE,
                    RabbitMQConfig.BATCH_MESSAGE_ROUTING_KEY,
                    event
                );
                
                if (success) {
                    successCount++;
                } else {
                    log.error("批量消息发送失败，messageId: {}", event.getMessageId());
                }
            }
            
            log.info("批量消息发送完成，总数: {}, 成功: {}, 失败: {}", 
                events.size(), successCount, events.size() - successCount);
        } catch (Exception e) {
            log.error("发送批量消息到队列失败", e);
            throw new RuntimeException("发送批量消息到队列失败: " + e.getMessage());
        }
    }

    /**
     * 发送会话更新事件到队列
     * 使用 globox-mq-utils 工具类
     */
    public void sendConversationUpdateToQueue(ConversationUpdateEvent event) {
        try {
            event.setCreatedAt(LocalDateTime.now());
            
            // 使用 MQUtils 工具类发送消息
            boolean success = mqService.send(
                RabbitMQConfig.SOCIAL_EXCHANGE,
                RabbitMQConfig.CONVERSATION_ROUTING_KEY,
                event
            );
            
            if (success) {
                log.info("会话更新事件已发送到队列，conversationId: {}, operationType: {}", 
                    event.getConversationId(), event.getOperationType());
            } else {
                log.error("会话更新事件发送失败，conversationId: {}", event.getConversationId());
                throw new RuntimeException("会话更新事件发送失败");
            }
        } catch (Exception e) {
            log.error("发送会话更新事件到队列失败", e);
            throw new RuntimeException("发送会话更新事件到队列失败: " + e.getMessage());
        }
    }

    /**
     * 发送延迟重试消息
     * 使用 globox-mq-utils 工具类的延迟消息功能
     * 
     * @param event 消息事件
     * @param delaySeconds 延迟时间（秒）
     */
    public void sendRetryMessage(MessageQueueEvent event, int delaySeconds) {
        try {
            event.setRetryCount(event.getRetryCount() != null ? event.getRetryCount() + 1 : 1);
            
            // 使用 MQUtils 工具类发送延迟消息
            boolean success = mqService.sendDelay(
                RabbitMQConfig.SOCIAL_EXCHANGE,
                RabbitMQConfig.MESSAGE_ROUTING_KEY,
                event,
                delaySeconds
            );
            
            if (success) {
                log.info("延迟重试消息已发送，messageId: {}, 重试次数: {}, 延迟: {}秒", 
                    event.getMessageId(), event.getRetryCount(), delaySeconds);
            } else {
                log.error("延迟重试消息发送失败，messageId: {}", event.getMessageId());
            }
        } catch (Exception e) {
            log.error("发送延迟重试消息失败", e);
        }
    }
}
