package com.unlimited.sports.globox.notification.dto.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;
import com.unlimited.sports.globox.notification.enums.MessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知消息视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageVO {

    /**
     * 记录ID
     */
    private Long recordId;

    /**
     * 通知ID（消息唯一标识）
     */
    private String notificationId;

    /**
     * 消息类型（用户端分类）
     * explore=探索消息, rally=球局消息, system=系统消息
     */
    private String messageType;

    /**
     * 模块代码（后端分类）
     * 3=约球, 4=社交, 5=系统
     */
    private Integer moduleCode;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 跳转链接（deeplink）
     */
    private String action;

    /**
     * 自定义数据
     */
    private Map<String, Object> customData;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 从 PushRecords 实体转换为 VO
     */
    public static NotificationMessageVO fromEntity(PushRecords record) {
        MessageTypeEnum messageType = MessageTypeEnum.fromModuleCode(record.getNotificationModule());

        return NotificationMessageVO.builder()
                .recordId(record.getRecordId())
                .notificationId(record.getNotificationId())
                .messageType(messageType != null ? messageType.getCode() : null)
                .moduleCode(record.getNotificationModule())
                .eventType(record.getEventType())
                .businessId(record.getBusinessId())
                .title(record.getTitle())
                .content(record.getContent())
                .action(record.getAction())
                .customData(record.getCustomData())
                .isRead(record.getIsRead() != null && record.getIsRead() == 1)
                .createdAt(record.getCreatedAt())
                .build();
    }
}
