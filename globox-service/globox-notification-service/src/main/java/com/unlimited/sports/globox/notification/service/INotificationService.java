package com.unlimited.sports.globox.notification.service;

import com.unlimited.sports.globox.common.message.notification.NotificationMessage;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;

import java.util.Map;

/**
 * 通知服务接口
 * 负责消息处理、推送和记录
 */
public interface INotificationService {

    /**
     * 处理通知消息（MQ消息到达时调用）
     * 包括：验证消息、获取用户设备、推送到腾讯云、记录推送结果
     *
     * @param message 通知消息
     */
    void handleNotification(NotificationMessage message);


    /**
     * 查询推送记录
     *
     * @param messageId 消息ID
     * @return 推送记录，如果不存在则返回null
     */
    PushRecords getPushRecord(String messageId);
}
