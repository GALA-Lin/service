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
     * 发送推送到腾讯云
     *
     * @param userId 用户ID
     * @param deviceToken 设备令牌
     * @param title 推送标题
     * @param content 推送内容
     * @param action deeplink
     * @param customData 自定义数据
     * @return 腾讯云返回的task_id
     */
    String sendToTencent(Long userId, String deviceToken, String title, String content,
                        String action, Map<String, Object> customData);

    /**
     * 记录推送结果
     *
     * @param record 推送记录
     */
    void recordPushResult(PushRecords record);

    /**
     * 查询推送记录
     *
     * @param messageId 消息ID
     * @return 推送记录，如果不存在则返回null
     */
    PushRecords getPushRecord(String messageId);
}
