package com.unlimited.sports.globox.notification.consumer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.NotificationMQConstants;
import com.unlimited.sports.globox.common.message.notification.DeviceActivationMessage;
import com.unlimited.sports.globox.notification.service.IDeviceTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备激活消费者
 * 处理用户登录时的设备Token同步
 *
 * 当用户登录时，用户服务发送设备激活消息到MQ
 * 通知服务接收消息并同步设备Token映射关系
 *
 * 重试策略：最大3次，重试间隔2秒
 */
@Slf4j
@Component
@RabbitListener(queues = NotificationMQConstants.QUEUE_DEVICE_ACTIVATION, concurrency = "3-5")
public class DeviceActivationConsumer {


    @Autowired
    private  IDeviceTokenService deviceTokenService;



    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            maxRetryCount = 3,
            finalExchange = NotificationMQConstants.EXCHANGE_DEVICE_ACTIVATION_FINAL_DLX,
            finalRoutingKey = NotificationMQConstants.ROUTING_DEVICE_ACTIVATION_FINAL
    )
    public void onMessage(DeviceActivationMessage message, Channel channel, Message amqpMessage) throws Exception {
        String messageId = message.getMessageId();
        Long userId = message.getUserId();
        String deviceId = message.getDeviceId();

        log.info("[设备激活] 收到消息: messageId={}, userId={}, deviceId={}, deviceOs={}",
                messageId, userId, deviceId, message.getDeviceOs());

        // 同步设备Token（包含单点登录逻辑）
        deviceTokenService.syncDeviceToken(
                message.getUserId(),
                message.getUserType(),
                message.getDeviceId(),
                message.getDeviceToken(),
                message.getDeviceOs()
        );

        log.info("[设备激活] 处理成功: messageId={}, userId={}, deviceId={}", messageId, userId, deviceId);
    }
}
