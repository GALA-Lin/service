package com.unlimited.sports.globox.notification.util;

import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.notification.enums.MessageTypeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 通知类型转换工具类
 * 将通知事件(NotificationEventEnum)转换为用户端消息类型(MessageTypeEnum)
 */
@Slf4j
public class NotificationTypeConverter {

    /**
     * 根据通知事件编码推断消息类型
     *
     * @param eventCode 事件编码，对应 NotificationEventEnum.getEventCode()
     * @return 消息类型，如果转换失败返回 null
     */
    public static MessageTypeEnum inferMessageType(String eventCode) {
        if (eventCode == null || eventCode.isEmpty()) {
            return null;
        }

        try {
            // 将事件编码转换为 NotificationEventEnum
            NotificationEventEnum event = NotificationEventEnum.valueOf(eventCode);

            // 根据事件的模块推断消息类型
            return MessageTypeEnum.fromModule(event.getModule());
        } catch (IllegalArgumentException e) {
            log.warn("[类型转换] 事件编码无效: eventCode={}, error={}", eventCode, e.getMessage());
            return null;
        }
    }

    /**
     * 根据通知事件推断消息类型
     *
     * @param event 通知事件枚举
     * @return 消息类型，如果转换失败返回 null
     */
    public static MessageTypeEnum inferMessageType(NotificationEventEnum event) {
        if (event == null) {
            return null;
        }
        return MessageTypeEnum.fromModule(event.getModule());
    }
}
