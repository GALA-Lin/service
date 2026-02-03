package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("conversation")
public class Conversation implements Serializable {
    
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(value = "conversation_id", type = IdType.AUTO)
    private Long conversationId;

    /**
     * 会话类型: 1-私信, 2-群聊, 3-系统消息
     */
    @TableField("conversation_type")
    private ConversationTypeEnum conversationType;

    /**
     * 发送方用户ID
     */
    @TableField("sender_user_id")
    private Long senderUserId;

    /**
     * 接收方用户ID
     */
    @TableField("receive_user_id")
    private Long receiveUserId;

    /**
     * 会话名称(发送方展示)
     */
    @TableField("conversation_name_sender")
    private String conversationNameSender;

    /**
     * 会话头像(发送方展示)
     */
    @TableField("conversation_avatar_sender")
    private String conversationAvatarSender;
    /**
     * 会话名称(接收方展示)
     */
    @TableField("conversation_name_receiver")
    private String conversationNameReceiver;

    /**
     * 会话头像(接收方展示)
     */
    @TableField("conversation_avatar_receiver")
    private String conversationAvatarReceiver;

    /**
     * 最后一条消息ID
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 最后一条消息内容
     */
    @TableField("last_message_content")
    private String lastMessageContent;

    /**
     * 最后一条消息类型
     */
    @TableField("last_message_type")
    private MessageTypeEnum lastMessageType;

    /**
     * 最后一条消息时间
     */
    @TableField("last_message_at")
    private LocalDateTime lastMessageAt;

    /**
     * 发送方未读计数
     */
    @TableField("unread_count_sender")
    private Long unreadCountSender;

    /**
     * 接收方未读计数
     */
    @TableField("unread_count_receiver")
    private Long unreadCountReceiver;

    /**
     * 是否屏蔽
     */
    @TableField("is_blocked")
    private Boolean isBlocked;

    /**
     * 屏蔽发起用户ID
     */
    @TableField("blocked_by_user_id")
    private Long blockedByUserId;

    /**
     * 发送方是否置顶
     */
    @TableField("is_pinned_sender")
    private Boolean isPinnedSender;

    /**
     * 接收方是否置顶
     */
    @TableField("is_pinned_receiver")
    private Boolean isPinnedReceiver;

    /**
     * 发送方是否删除
     */
    @TableField("is_deleted_sender")
    private Boolean isDeletedSender;

    /**
     * 接收方是否删除
     */
    @TableField("is_deleted_receiver")
    private Boolean isDeletedReceiver;

    /**
     * 扩展信息(JSON格式)
     */
    @TableField("extra")
    private String extra;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
