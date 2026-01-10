package com.unlimited.sports.globox.model.social.vo;

import com.unlimited.sports.globox.model.social.entity.ConversationTypeEnum;
import com.unlimited.sports.globox.model.social.entity.MessageTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationVo {
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 会话类型: 1-私信, 2-群聊, 3-系统消息
     */
    private ConversationTypeEnum conversationType;

    /**
     * 接收方用户ID
     */
    private Long receiveUserId;
    /**
     * 会话名称(接收方展示)
     */

    private String conversationNameReceiver;

    /**
     * 会话头像(接收方展示)
     */

    private String conversationAvatarReceiver;
    /**
     * 最后一条消息ID
     */

    private Long lastMessageId;

    /**
     * 最后一条消息内容
     */

    private String lastMessageContent;

    /**
     * 最后一条消息类型
     */

    private MessageTypeEnum lastMessageType;

    /**
     * 最后一条消息时间
     */

    private LocalDateTime lastMessageAt;

    /**
     * 发送方未读计数
     */

    private Long unreadCountSender;

    /**
     * 接收方未读计数
     */

    private Long unreadCountReceiver;

    /**
     * 是否屏蔽
     */

    private Boolean isBlocked;

    /**
     * 屏蔽发起用户ID
     */

    private Long blockedByUserId;

    /**
     * 发送方是否置顶
     */
    private Boolean isPinnedSender;

    /**
     * 接收方是否置顶
     */
    private Boolean isPinnedReceiver;

    /**
     * 发送方是否删除
     */
    private Boolean isDeletedSender;

    /**
     * 接收方是否删除
     */
    private Boolean isDeletedReceiver;

    /**
     * 扩展信息(JSON格式)
     */
    private String extra;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
