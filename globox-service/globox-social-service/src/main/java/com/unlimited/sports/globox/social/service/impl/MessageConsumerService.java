package com.unlimited.sports.globox.social.service.impl;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.utils.IdGenerator;
import com.unlimited.sports.globox.model.social.entity.*;
import com.unlimited.sports.globox.social.config.RabbitMQConfig;
import com.unlimited.sports.globox.social.mapper.MessageMapper;
import com.unlimited.sports.globox.social.service.ConversationService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 消息消费者服务（MANUAL ACK）
 *
 * 特点：
 * 1. 数据库成功 → ACK
 * 2. 失败 → NACK 重回队列
 * 3. 超过最大重试次数 → Reject（进入 DLQ）
 */
@Slf4j
@Service
public class MessageConsumerService {

    private static final int MAX_RETRY_COUNT = 3;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private MessageProducerService messageProducerService;

    /**
     * 消费单条消息保存事件
     */
    @RabbitListener(
            queues = RabbitMQConfig.MESSAGE_QUEUE,
            ackMode = "MANUAL"
    )
    @Transactional(rollbackFor = Exception.class)
    public void consumeMessage(
            MessageQueueEvent event,
            Message message,
            Channel channel
    ) throws IOException {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info(
                    "开始消费消息，deliveryTag={}, messageId={}, fromUserId={}, toUserId={}",
                    deliveryTag, event.getMessageId(), event.getFromUserId(), event.getToUserId()
            );

            // 1️ 幂等校验（messageId 已存在直接 ACK）
            if (event.getMessageId() != null) {
                MessageEntity existing = messageMapper.selectById(event.getMessageId());
                existing = existing;
                if (existing != null) {
                    log.warn("消息已存在，直接 ACK，messageId={}", event.getMessageId());
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            // 2️ 生成 messageId
            if (event.getMessageId() == null) {
                event.setMessageId(idGenerator.nextId());
            }

            // 3️ 构建消息实体
            MessageEntity messageEntity = MessageEntity.builder()
                    .messageId(event.getMessageId())
                    .fromUserId(event.getFromUserId())
                    .toUserId(event.getToUserId())
                    .conversationId(event.getConversationId())
                    .messageType(event.getMessageType())
                    .content(event.getContent())
                    .status(event.getStatus() != null
                            ? event.getStatus()
                            : MessageStatusEnum.SENT)
                    .isRead(event.getIsRead() != null && event.getIsRead())
                    .sendTime(event.getSendTime() != null
                            ? event.getSendTime()
                            : LocalDateTime.now())
                    .random(event.getRandom())
                    .extra(event.getExtra())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 4️ 入库
            messageMapper.insert(messageEntity);
            log.info("消息入库成功，messageId={}", messageEntity.getMessageId());

            // 5️ 发送会话更新事件（异步、最终一致）
            sendConversationUpdateEvent(event);

            // 6️ 手动 ACK（非常关键）
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {

            Integer retryCount = event.getRetryCount() == null ? 0 : event.getRetryCount();
            retryCount++;
            event.setRetryCount(retryCount);

            log.error(
                    "消息消费失败，messageId={}, retryCount={}",
                    event.getMessageId(), retryCount, e
            );

            if (retryCount <= MAX_RETRY_COUNT) {
                //  NACK 并重新入队
                channel.basicNack(deliveryTag, false, true);
            } else {
                //  超过重试次数，拒绝消息（进入死信队列）
                log.error("消息超过最大重试次数，进入 DLQ，messageId={}", event.getMessageId());
                channel.basicReject(deliveryTag, false);
            }

            // 抛出异常，触发事务回滚（仅 DB）
            throw e;
        }
    }

    /**
     * 会话更新事件消费
     * 会话更新失败 ≠ 消息消费失败
     */
    @RabbitListener(
            queues = RabbitMQConfig.CONVERSATION_QUEUE,
            ackMode = "MANUAL"
    )
    @Transactional(rollbackFor = Exception.class)
    public void consumeConversationUpdate(
            ConversationUpdateEvent event,
            Message message,
            Channel channel
    ) throws IOException {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            log.info(
                    "处理会话更新事件，conversationId={}, operationType={}",
                    event.getConversationId(), event.getOperationType()
            );

            Conversation conversation =
                    conversationService.getConversationById(event.getConversationId());

            if (conversation == null) {
                log.warn("会话不存在，直接 ACK，conversationId={}", event.getConversationId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            if ("UPDATE_LAST_MESSAGE".equals(event.getOperationType())) {
                conversationService.updateLastMessage(
                        event.getConversationId(),
                        event.getLastMessageId(),
                        event.getLastMessageContent(),
                        event.getLastMessageType()
                );
            }

            if ("INCREMENT_UNREAD".equals(event.getOperationType())
                    && Boolean.TRUE.equals(event.getNeedIncrementUnread())) {
                conversationService.incrementUnreadCount(
                        event.getConversationId(),
                        event.getFromUserId(),
                        event.getToUserId()
                );
            }

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("会话更新失败，conversationId={}", event.getConversationId(), e);
            channel.basicNack(deliveryTag, false, true);
            throw e;
        }
    }

    /**
     * 发送会话更新事件
     */
    private void sendConversationUpdateEvent(MessageQueueEvent messageEvent) {

        // 更新最后一条消息
        ConversationUpdateEvent updateEvent = ConversationUpdateEvent.builder()
                .conversationId(messageEvent.getConversationId())
                .lastMessageId(messageEvent.getMessageId())
                .lastMessageContent(messageEvent.getContent())
                .lastMessageType(messageEvent.getMessageType())
                .fromUserId(messageEvent.getFromUserId())
                .toUserId(messageEvent.getToUserId())
                .operationType(OperationTypeEnum.UPDATE_LAST_MESSAGE)
                .build();

        messageProducerService.sendConversationUpdateToQueue(updateEvent);

        // 未读数递增
        if (messageEvent.getIsRead() == null || !messageEvent.getIsRead()) {
            ConversationUpdateEvent unreadEvent = ConversationUpdateEvent.builder()
                    .conversationId(messageEvent.getConversationId())
                    .fromUserId(messageEvent.getFromUserId())
                    .toUserId(messageEvent.getToUserId())
                    .needIncrementUnread(true)
                    .operationType(OperationTypeEnum.INCREMENT_UNREAD)
                    .build();

            messageProducerService.sendConversationUpdateToQueue(unreadEvent);
        }
    }
}
