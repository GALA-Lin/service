package com.unlimited.sports.globox.order.util;

import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 场地订单通知工具类（订单服务）
 * 负责发送场地订单相关的通知消息到通知服务
 */
@Slf4j
@Component
public class VenueNotificationHelper {

    @Autowired
    private NotificationSender notificationSender;

    /**
     * 发送VENUE_REFUND_SUCCESS通知（退款成功到账，通知用户"退款已到账"）
     */
    public void sendVenueRefundSuccess(Long orderNo, Long buyerId, BigDecimal refundAmount) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("refundAmount", refundAmount.toString());
            customData.put("refundCompletedAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(buyerId, NotificationEventEnum.VENUE_REFUND_SUCCESS, orderNo, customData);
            log.info("[Venue通知] 退款成功到账，通知用户, orderNo={}, buyerId={}, refundAmount={}", orderNo, buyerId, refundAmount);
        } catch (Exception e) {
            log.error("[Venue通知] 退款成功通知发送失败, orderNo={}, buyerId={}", orderNo, buyerId, e);
        }
    }
}
