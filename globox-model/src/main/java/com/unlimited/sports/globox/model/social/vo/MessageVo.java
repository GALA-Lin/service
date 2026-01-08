package com.unlimited.sports.globox.model.social.vo;


import com.unlimited.sports.globox.model.social.entity.MessageStatusEnum;
import com.unlimited.sports.globox.model.social.entity.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageVo {
    private Long messageId;

    /**
     * 发送方用户ID
     */
    private Long fromUserId;

    /**
     * 发送方用户名称
     */
    private String fromUserName;

    /**
     * 发送方用户头像
     */
    private String fromUserAvatar;

    /**
     * 接收方用户ID
     */
    private Long toUserId;

    /**
     * 接收方用户名称
     */
    private String toUserName;

    /**
     * 接收方用户头像
     */
    private String toUserAvatar;

    /**
     * 消息类型: 1-文本, 2-图像, 3-音频, 4-视频, 5-文件, 6-地址
     */
    private MessageTypeEnum messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息状态: 1-已读, 2-已发送, 3-送达, 4-已撤回, 5-已删除, 6-失败
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
     * 接收时间
     */
    private LocalDateTime receiveTime;

    /**
     * 已读时间
     */
    private LocalDateTime readTime;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 消息随机值(用于去重)
     */
    private Long random;

    /**
     * 是否已撤回
     */
    private Boolean isRecalled;

    /**
     * 撤回时间
     */
    private LocalDateTime recalledAt;

    /**
     * 发送者是否删除
     */
    private Boolean isDeletedBySender;

    /**
     * 接收者是否删除
     */
    private Boolean isDeletedByReceiver;

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
