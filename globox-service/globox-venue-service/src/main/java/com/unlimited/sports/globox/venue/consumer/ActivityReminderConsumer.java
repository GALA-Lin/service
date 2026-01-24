package com.unlimited.sports.globox.venue.consumer;

import com.rabbitmq.client.Channel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.VenueMQConstants;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.message.venue.ActivityReminderMessage;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.model.venue.enums.VenueActivityStatusEnum;
import com.unlimited.sports.globox.venue.constants.ActivityParticipantConstants;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 活动提醒延迟消息消费者
 * 消费延迟消息，验证参与者记录后发送通知
 *
 * 流程：
 * 1. 接收延迟消息（包含userId、participantId和registrationTime）
 * 2. 验证参与者记录仍然有效
 * 3. 查询活动、场馆、场地信息
 * 4. 发送通知到notification-service
 */
@Slf4j
@Component
@RabbitListener(queues = VenueMQConstants.QUEUE_ACTIVITY_BOOKING_REMINDER, concurrency = "3-5")
public class ActivityReminderConsumer {

    @Autowired
    private VenueActivityParticipantMapper participantMapper;

    @Autowired
    private VenueActivityMapper activityMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private NotificationSender notificationSender;

    /**
     * 处理活动提醒延迟消息
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(ActivityReminderMessage message, Channel channel, Message amqpMessage) throws IOException {
        Long userId = message.getUserId();
        Long participantId = message.getParticipantId();
        LocalDateTime registrationTime = message.getRegistrationTime();

        log.info("[活动提醒] 接收到延迟消息 - userId={}, participantId={}, registrationTime={}",
                userId, participantId, registrationTime);

        // 校验参数
        if (participantId == null) {
            log.warn("[活动提醒] 参与者ID为空 - userId={}", userId);
            return;
        }

        if (registrationTime == null) {
            log.warn("[活动提醒] 报名时间为空 - userId={}, participantId={}", userId, participantId);
            return;
        }

        try {
            // 通过SQL查询验证：参与者ID匹配、userId匹配、registrationTime匹配、未被取消
            VenueActivityParticipant participant = participantMapper.selectOne(
                    Wrappers.lambdaQuery(VenueActivityParticipant.class)
                            .eq(VenueActivityParticipant::getParticipantId, participantId)
                            .eq(VenueActivityParticipant::getUserId, userId)
                            .eq(VenueActivityParticipant::getCreatedAt, registrationTime)
                            .eq(VenueActivityParticipant::getDeleteVersion, ActivityParticipantConstants.DELETE_VERSION_ACTIVE)
            );

            // 验证参与者记录仍然存在且未被取消
            if (participant == null) {
                log.info("[活动提醒] 参与者记录已取消或不存在，跳过通知 - userId={}, participantId={}",
                        userId, participantId);
                return;
            }

            // 查询活动信息
            VenueActivity activity = activityMapper.selectById(participant.getActivityId());
            if (activity == null || VenueActivityStatusEnum.CANCELLED.getValue().equals(activity.getStatus())) {
                log.warn("[活动提醒] 活动不存在 - activityId={}", participant.getActivityId());
                return;
            }

            // 查询场地信息
            Court court = courtMapper.selectById(activity.getCourtId());
            if (court == null) {
                log.warn("[活动提醒] 场地不存在 - courtId={}", activity.getCourtId());
                return;
            }

            // 查询场馆信息
            Venue venue = venueMapper.selectById(activity.getVenueId());
            if (venue == null) {
                log.warn("[活动提醒] 场馆不存在 - venueId={}", activity.getVenueId());
                return;
            }

            // 使用通用工具发送通知
            notificationSender.sendNotification(
                    userId,
                    NotificationEventEnum.ACTIVITY_BOOKING_REMINDER,
                    participantId,
                    NotificationSender.createCustomData()
                            .put("venueId", venue.getVenueId())
                            .put("venueName", venue.getName())
                            .put("courtId", court.getCourtId())
                            .put("courtName", court.getName())
                            .put("activityName", activity.getActivityName())
                            .put("activityDate", activity.getActivityDate())
                            .put("startTime", activity.getStartTime())
                            .put("endTime", activity.getEndTime())
                            .put("orderNo", message.getOrderNo())
                            .put("sellerType", SellerTypeEnum.VENUE.getCode())
                            .build()
            );

            log.info("[活动提醒] 通知发送成功 - userId={}, activityName={}, 活动时间={}-{}",
                    userId, activity.getActivityName(),
                    activity.getStartTime(), activity.getEndTime());

        } catch (Exception e) {
            log.error("[活动提醒] 处理失败 - userId={}, participantId={}", userId, participantId, e);
            throw e;
        } finally {
            long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
        }
    }
}
