package com.unlimited.sports.globox.model.social.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("message")
public class MessageEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    /**
     * 发送方用户ID
     */
    @TableField("from_user_id")
    private Long fromUserId;

    /**
     * 接收方用户ID
     */
    @TableField("to_user_id")
    private Long toUserId;

    /**
     * 消息类型: 1-文本, 2-图像, 3-音频, 4-视频, 5-文件, 6-地址
     */
    @TableField("message_type")
    private MessageTypeEnum messageType;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息状态: 1-已读, 2-已发送, 3-送达, 4-已撤回, 5-已删除, 6-失败
     */
    @TableField("status")
    private MessageStatusEnum status;

    /**
     * 是否已读
     */
    @TableField("is_read")
    private Boolean isRead;

    /**
     * 发送时间
     */
    @TableField("send_time")
    private LocalDateTime sendTime;

    /**
     * 接收时间
     */
    @TableField("receive_time")
    private LocalDateTime receiveTime;

    /**
     * 已读时间
     */
    @TableField("read_time")
    private LocalDateTime readTime;

    /**
     * 会话ID
     */
    @TableField("conversation_id")
    private Long conversationId;

    /**
     * 消息随机值(用于去重)
     */
    @TableField("random")
    private Long random;

    /**
     * 是否已撤回
     */
    @TableField("is_recalled")
    private Boolean isRecalled;

    /**
     * 撤回时间
     */
    @TableField("recalled_at")
    private LocalDateTime recalledAt;

    /**
     * 发送者是否删除
     */
    @TableField("is_deleted_by_sender")
    private Boolean isDeletedBySender;

    /**
     * 接收者是否删除
     */
    @TableField("is_deleted_by_receiver")
    private Boolean isDeletedByReceiver;

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
