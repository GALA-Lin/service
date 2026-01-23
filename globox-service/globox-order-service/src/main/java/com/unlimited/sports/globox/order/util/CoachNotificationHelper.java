package com.unlimited.sports.globox.order.util;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.enums.notification.NotificationEntityTypeEnum;
import com.unlimited.sports.globox.common.message.coach.CoachClassReminderMessage;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.model.order.entity.OrderItems;
import com.unlimited.sports.globox.order.mapper.OrderItemsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教练订单通知工具类（订单服务）
 * 负责发送教练相关的通知消息到通知服务
 */
@Slf4j
@Component
public class CoachNotificationHelper {

    @Autowired
    private NotificationSender notificationSender;


    /**
     * 发送COACH_PROVIDER_APPOINTMENT_CREATED通知（订单支付成功，通知教练"您收到新的预约"，展示学员信息）
     */
    public void sendCoachAppointmentPaid(Long orderNo, Long coachId, Long buyerId, String bookingDate, BigDecimal totalAmount) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("bookingDate", bookingDate);
            customData.put("totalAmount", totalAmount.toString());

            notificationSender.sendNotification(coachId, NotificationEventEnum.COACH_PROVIDER_APPOINTMENT_CREATED, orderNo, customData,
                    NotificationEntityTypeEnum.USER, buyerId);
            log.info("[Coach通知] 订单支付成功，发送新预约通知给教练, orderNo={}, coachId={}, buyerId={}", orderNo, coachId, buyerId);
        } catch (Exception e) {
            log.error("[Coach通知] 订单支付成功通知发送失败, orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }

    /**
     * 发送COACH_REFUND_REQUEST通知（学员申请退款，通知教练收到退款申请，展示学员信息）
     */
    public void sendCoachRefundRequest(Long orderNo, Long coachId, Long buyerId) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("buyerId", buyerId);
            customData.put("requestAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(coachId, NotificationEventEnum.COACH_REFUND_REQUEST, orderNo, customData,
                    NotificationEntityTypeEnum.USER, buyerId);
            log.info("[Coach通知] 学员申请退款，通知教练, orderNo={}, coachId={}, buyerId={}", orderNo, coachId, buyerId);
        } catch (Exception e) {
            log.error("[Coach通知] 退款申请通知发送失败, orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }



    /**
     * 发送COACH_PROVIDER_APPOINTMENT_CANCELLED通知（学员直接退款取消未确认订单，通知教练订单已被取消，展示学员信息）
     */
    public void sendCoachOrderCancelledByStudent(Long orderNo, Long coachId, Long buyerId) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("cancelledAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(coachId, NotificationEventEnum.COACH_PROVIDER_APPOINTMENT_CANCELLED, orderNo, customData,
                    NotificationEntityTypeEnum.USER, buyerId);
            log.info("[Coach通知] 学员直接退款取消未确认订单，通知教练, orderNo={}, coachId={}, buyerId={}", orderNo, coachId, buyerId);
        } catch (Exception e) {
            log.error("[Coach通知] 订单已被学员取消通知发送失败, orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }

    /**
     * 发送COACH_REFUND_SUCCESS通知（退款成功到账，通知学员"退款已到账"）
     */
    public void sendCoachRefundSuccess(Long orderNo, Long buyerId, BigDecimal refundAmount) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("refundAmount", refundAmount.toString());
            customData.put("refundCompletedAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(buyerId, NotificationEventEnum.COACH_REFUND_SUCCESS, orderNo, customData);
            log.info("[Coach通知] 退款成功到账，通知学员, orderNo={}, buyerId={}, refundAmount={}", orderNo, buyerId, refundAmount);
        } catch (Exception e) {
            log.error("[Coach通知] 退款成功通知发送失败, orderNo={}, buyerId={}", orderNo, buyerId, e);
        }
    }

}
