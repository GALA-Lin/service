package com.unlimited.sports.globox.coach.util;

import com.unlimited.sports.globox.common.constants.CoachMQConstants;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.enums.notification.NotificationEntityTypeEnum;
import com.unlimited.sports.globox.common.message.coach.CoachClassReminderMessage;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.coach.constants.CoachConstants;
import com.unlimited.sports.globox.dubbo.order.OrderForCoachDubboService;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderDetailsRequestDto;
import com.unlimited.sports.globox.dubbo.order.dto.CoachGetOrderResultDto;
import com.unlimited.sports.globox.dubbo.order.dto.RecordDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教练通知工具类
 * 负责发送教练和学员相关的通知消息（仅发送，不做查询）
 */
@Slf4j
@Component
public class CoachNotificationUtil {

    @Autowired
    private NotificationSender notificationSender;

    @Autowired
    private MQService mqService;

    @DubboReference(group = "rpc", timeout = CoachConstants.DUBBO_RPC_TIMEOUT)
    private OrderForCoachDubboService orderForCoachDubboService;

    /**
     * 课程开始前多久提醒（单位：秒）
     */
    @Value("${coach.class.reminder.advance.seconds:3600}")
    private int reminderAdvanceSeconds;


    /**
     * 发送课程提醒通知（给教练和学员）
     *
     * @param orderNo 订单号
     * @param coachId 教练ID
     * @param buyerId 学员ID
     * @param coachName 教练名称
     */
    public void sendCoachClassReminderNotification(Long orderNo, Long coachId, Long buyerId, String coachName) {
        try {
            // 1. 发送提醒通知给教练
            Map<String, Object> coachCustomData = new HashMap<>();
            coachCustomData.put("orderNo", orderNo);
            coachCustomData.put("reminderType", "PROVIDER");

            notificationSender.sendNotification(coachId, NotificationEventEnum.COACH_CLASS_PROVIDER_REMINDER, orderNo, coachCustomData);

            // 2. 发送提醒通知给学员
            Map<String, Object> bookerCustomData = new HashMap<>();
            bookerCustomData.put("orderNo", orderNo);
            bookerCustomData.put("coachName", coachName);
            bookerCustomData.put("reminderType", "BOOKER");

            notificationSender.sendNotification(buyerId, NotificationEventEnum.COACH_CLASS_BOOKER_REMINDER, orderNo, bookerCustomData);

            log.info("[课程提醒] 通知发送成功 - orderNo: {}, coachId: {}, buyerId: {}", orderNo, coachId, buyerId);
        } catch (Exception e) {
            log.error("[课程提醒] 通知发送失败 - orderNo: {}, coachId: {}, buyerId: {}", orderNo, coachId, buyerId, e);
        }
    }

    /**
     * 发送课程提醒延迟消息
     * 在教练确认订单后调用，使用已获取的订单详情发送延迟消息
     *
     * @param orderDetail 订单详情对象
     */
    public void sendClassReminderDelayMessage(CoachGetOrderResultDto orderDetail) {
        try {
            Long orderNo = orderDetail.getOrderNo();
            Long coachId = orderDetail.getCoachId();
            List<RecordDto> records = orderDetail.getRecords();

            if (records == null || records.isEmpty()) {
                log.warn("[课程提醒延迟消息] 订单项为空 - orderNo={}", orderNo);
                return;
            }

            // 1. 获取最早的课程开始时间
            RecordDto firstRecord = records.stream()
                    .min(Comparator.comparing(r -> LocalDateTime.of(r.getBookingDate(), r.getStartTime())))
                    .orElse(null);

            LocalDateTime classStartTime = LocalDateTime.of(
                    firstRecord.getBookingDate(),
                    firstRecord.getStartTime());

            // 2. 计算延迟时间（提前指定秒数提醒）
            LocalDateTime reminderTime = classStartTime.minusSeconds(reminderAdvanceSeconds);
            long delayMillis = Duration.between(LocalDateTime.now(), reminderTime).toMillis();

            // 3. 只有当延迟时间大于0时才发送（即提醒时间在未来）
            if (delayMillis > 0) {
                CoachClassReminderMessage message = CoachClassReminderMessage.builder()
                        .orderNo(orderNo)
                        .coachId(coachId)
                        .buyerId(orderDetail.getUserId())
                        .coachName(orderDetail.getCoachName())
                        .build();

                mqService.sendDelay(
                        CoachMQConstants.EXCHANGE_TOPIC_COACH_CLASS_REMINDER,
                        CoachMQConstants.ROUTING_COACH_CLASS_REMINDER,
                        message,
                        (int)delayMillis);

                log.info("[课程提醒延迟消息] 已发送 - orderNo={}, coachId={}, delayMillis={}, reminderTime={}",
                        orderNo, coachId, delayMillis, reminderTime);
            } else {
                log.warn("[课程提醒延迟消息] 提醒时间已过 - orderNo={}, reminderTime={}", orderNo, reminderTime);
            }
        } catch (Exception e) {
            log.error("[课程提醒延迟消息] 发送失败 - orderNo={}", orderDetail.getOrderNo(), e);
        }
    }

    /**
     * 教练取消未支付订单后发送通知给学员
     */
    public void handleCoachCancelUnpaidOrderNotification(Long orderNo, Long coachId) {
        try {
            // 获取订单详情
            CoachGetOrderDetailsRequestDto request = CoachGetOrderDetailsRequestDto.builder()
                    .orderNo(orderNo)
                    .coachId(coachId)
                    .build();
            RpcResult<CoachGetOrderResultDto> rpcResult = orderForCoachDubboService.getOrderDetails(request);

            if (!rpcResult.isSuccess() || rpcResult.getData() == null) {
                log.warn("[教练取消订单通知] 获取订单详情失败 - orderNo={}, coachId={}", orderNo, coachId);
                return;
            }

            CoachGetOrderResultDto orderDetail = rpcResult.getData();
            // 发送通知给学员：订单已被教练取消
            sendCoachOrderCancelledByProvider(orderNo, orderDetail.getUserId(), orderDetail.getCoachId());
        } catch (Exception e) {
            log.error("[教练取消订单通知] 发送失败 - orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }

    /**
     * 教练确认订单后发送通知给学员并发送课程提醒延迟消息
     */
    public void handleCoachConfirmOrderNotification(Long orderNo, Long coachId) {
        try {
            // 获取订单详情
            CoachGetOrderDetailsRequestDto request = CoachGetOrderDetailsRequestDto.builder()
                    .orderNo(orderNo)
                    .coachId(coachId)
                    .build();
            RpcResult<CoachGetOrderResultDto> rpcResult = orderForCoachDubboService.getOrderDetails(request);

            if (!rpcResult.isSuccess() || rpcResult.getData() == null) {
                log.warn("[教练确认订单通知] 获取订单详情失败 - orderNo={}, coachId={}", orderNo, coachId);
                return;
            }

            CoachGetOrderResultDto orderDetail = rpcResult.getData();
            // 发送确认通知给学员
            sendCoachAppointmentConfirmed(orderNo, orderDetail.getUserId(), orderDetail.getCoachId(), orderDetail.getCoachName());

            // 发送课程提醒延迟消息
            sendClassReminderDelayMessage(orderDetail);
        } catch (Exception e) {
            log.error("[教练确认订单通知] 发送失败 - orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }

    /**
     * 教练同意退款后发送通知给学员
     */
    public void handleCoachApproveRefundNotification(Long orderNo, Long coachId) {
        try {
            // 获取订单详情
            CoachGetOrderDetailsRequestDto request = CoachGetOrderDetailsRequestDto.builder()
                    .orderNo(orderNo)
                    .coachId(coachId)
                    .build();
            RpcResult<CoachGetOrderResultDto> rpcResult = orderForCoachDubboService.getOrderDetails(request);

            if (!rpcResult.isSuccess() || rpcResult.getData() == null) {
                log.warn("[教练同意退款通知] 获取订单详情失败 - orderNo={}, coachId={}", orderNo, coachId);
                return;
            }

            CoachGetOrderResultDto orderDetail = rpcResult.getData();
            // 发送申请通过通知给学员
            sendCoachRefundApproved(orderNo, orderDetail.getUserId(), orderDetail.getCoachId());
        } catch (Exception e) {
            log.error("[教练同意退款通知] 发送失败 - orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }

    /**
     * 教练拒绝退款后发送通知给学员
     */
    public void handleCoachRejectRefundNotification(Long orderNo, Long coachId, String remark) {
        try {
            // 获取订单详情
            CoachGetOrderDetailsRequestDto request = CoachGetOrderDetailsRequestDto.builder()
                    .orderNo(orderNo)
                    .coachId(coachId)
                    .build();
            RpcResult<CoachGetOrderResultDto> rpcResult = orderForCoachDubboService.getOrderDetails(request);

            if (!rpcResult.isSuccess() || rpcResult.getData() == null) {
                log.warn("[教练拒绝退款通知] 获取订单详情失败 - orderNo={}, coachId={}", orderNo, coachId);
                return;
            }

            CoachGetOrderResultDto orderDetail = rpcResult.getData();
            // 发送退款拒绝通知给学员
            sendCoachRefundRejected(orderNo, orderDetail.getUserId(), orderDetail.getCoachId(), remark);
        } catch (Exception e) {
            log.error("[教练拒绝退款通知] 发送失败 - orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }

    /**
     * 教练发起退款后发送通知给学员
     */
    public void handleCoachRefundNotification(Long orderNo, Long coachId) {
        try {
            // 获取订单详情
            CoachGetOrderDetailsRequestDto request = CoachGetOrderDetailsRequestDto.builder()
                    .orderNo(orderNo)
                    .coachId(coachId)
                    .build();
            RpcResult<CoachGetOrderResultDto> rpcResult = orderForCoachDubboService.getOrderDetails(request);

            if (!rpcResult.isSuccess() || rpcResult.getData() == null) {
                log.warn("[教练发起退款通知] 获取订单详情失败 - orderNo={}, coachId={}", orderNo, coachId);
                return;
            }

            CoachGetOrderResultDto orderDetail = rpcResult.getData();
            // 发送订单被教练取消通知给学员
            sendCoachOrderCancelledByProvider(orderNo, orderDetail.getUserId(), orderDetail.getCoachId());
        } catch (Exception e) {
            log.error("[教练发起退款通知] 发送失败 - orderNo={}, coachId={}", orderNo, coachId, e);
        }
    }

    /**
     * 发送COACH_APPOINTMENT_CONFIRMED通知（订单确认后发送给学员，展示教练信息）
     */
    public void sendCoachAppointmentConfirmed(Long orderNo, Long studentId, Long coachId, String coachName) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("coachName", coachName);

            notificationSender.sendNotification(studentId, NotificationEventEnum.COACH_APPOINTMENT_CONFIRMED, orderNo, customData,
                    NotificationEntityTypeEnum.USER, coachId);
            log.info("[学员通知] 订单确认通知发送成功 - orderNo={}, studentId={}, coachId={}", orderNo, studentId, coachId);
        } catch (Exception e) {
            log.error("[学员通知] 订单确认通知发送失败 - orderNo={}, studentId={}", orderNo, studentId, e);
        }
    }

    /**
     * 发送COACH_REFUND_APPROVED通知（退款批准后发送给学员，展示教练信息）
     */
    public void sendCoachRefundApproved(Long orderNo, Long studentId, Long coachId) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("approvedAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(studentId, NotificationEventEnum.COACH_REFUND_APPROVED, orderNo, customData,
                    NotificationEntityTypeEnum.USER, coachId);
            log.info("[学员通知] 退款批准通知发送成功 - orderNo={}, studentId={}, coachId={}", orderNo, studentId, coachId);
        } catch (Exception e) {
            log.error("[学员通知] 退款批准通知发送失败 - orderNo={}, studentId={}", orderNo, studentId, e);
        }
    }

    /**
     * 发送COACH_REFUND_REJECTED通知（退款拒绝后发送给学员，展示教练信息）
     */
    public void sendCoachRefundRejected(Long orderNo, Long studentId, Long coachId, String remark) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("rejectedAt", LocalDateTime.now().toString());
            customData.put("remark", remark != null ? remark : "教练拒绝退款申请");

            notificationSender.sendNotification(studentId, NotificationEventEnum.COACH_REFUND_REJECTED, orderNo, customData,
                    NotificationEntityTypeEnum.USER, coachId);
            log.info("[学员通知] 退款拒绝通知发送成功 - orderNo={}, studentId={}, coachId={}", orderNo, studentId, coachId);
        } catch (Exception e) {
            log.error("[学员通知] 退款拒绝通知发送失败 - orderNo={}, studentId={},coachId={}", orderNo, studentId,coachId, e);
        }
    }

    /**
     * 发送COACH_ORDER_CANCELLED_BY_PROVIDER通知（教练直接退款取消订单后发送给学员，展示教练信息）
     */
    public void sendCoachOrderCancelledByProvider(Long orderNo, Long studentId, Long coachId) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("cancelledAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(studentId, NotificationEventEnum.COACH_ORDER_CANCELLED_BY_PROVIDER, orderNo, customData,
                    NotificationEntityTypeEnum.USER, coachId);
            log.info("[学员通知] 订单被教练取消通知发送成功 - orderNo={}, studentId={}, coachId={}", orderNo, studentId, coachId);
        } catch (Exception e) {
            log.error("[学员通知] 订单被教练取消通知发送失败 - orderNo={}, studentId={}", orderNo, studentId, e);
        }
    }

    /**
     * 发送COACH_PROVIDER_APPOINTMENT_CANCELLED通知（教练取消未支付订单后发送给学员，展示教练信息）
     */
    public void sendCoachAppointmentCancelled(Long orderNo, Long studentId, Long coachId, String coachName) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("orderNo", orderNo);
            customData.put("coachName", coachName);
            customData.put("cancelledAt", LocalDateTime.now().toString());

            notificationSender.sendNotification(studentId, NotificationEventEnum.COACH_PROVIDER_APPOINTMENT_CANCELLED, orderNo, customData,
                    NotificationEntityTypeEnum.USER, coachId);
            log.info("[学员通知] 订单取消通知发送成功 - orderNo={}, studentId={}, coachId={}", orderNo, studentId, coachId);
        } catch (Exception e) {
            log.error("[学员通知] 订单取消通知发送失败 - orderNo={}, studentId={}", orderNo, studentId, e);
        }
    }
}
