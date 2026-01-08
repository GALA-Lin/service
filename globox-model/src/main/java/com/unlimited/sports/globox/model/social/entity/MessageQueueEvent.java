package com.unlimited.sports.globox.model.social.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息队列事件实体
 * 用于RabbitMQ消息传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueueEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 发送方用户ID
     */
    private Long fromUserId;

    /**
     * 接收方用户ID
     */
    private Long toUserId;

    /**
     * 消息类型
     */
    private MessageTypeEnum messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息状态
     */
    private MessageStatusEnum status;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 消息随机值(用于去重)
     */
    private Long random;

    /**
     * 扩展信息(JSON格式)
     */
    private String extra;

    /**
     * 操作类型：SAVE_MESSAGE, UPDATE_CONVERSATION, BATCH_SAVE
     */
    private String operationType;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
