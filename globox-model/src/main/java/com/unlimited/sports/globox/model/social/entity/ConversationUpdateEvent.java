package com.unlimited.sports.globox.model.social.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话更新事件实体
 * 用于RabbitMQ消息传输
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdateEvent implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 最后消息ID
     */
    private Long lastMessageId;

    /**
     * 最后消息内容
     */
    private String lastMessageContent;

    /**
     * 最后消息类型
     */
    private MessageTypeEnum lastMessageType;

    /**
     * 发送方用户ID
     */
    private Long fromUserId;

    /**
     * 接收方用户ID
     */
    private Long toUserId;

    /**
     * 是否需要增加未读计数
     */
    private Boolean needIncrementUnread;

    /**
     * 操作类型：UPDATE_LAST_MESSAGE, INCREMENT_UNREAD, CLEAR_UNREAD
     */
    private OperationTypeEnum operationType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
