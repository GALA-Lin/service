package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.constants.VenueMQConstants;
import com.unlimited.sports.globox.common.message.venue.ActivityReminderMessage;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipantStatusEnum;
import com.unlimited.sports.globox.model.venue.enums.VenueActivityStatusEnum;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import com.unlimited.sports.globox.venue.service.IActivityReminderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 活动提醒服务实现
 * 根据活动ID和参与者ID发送延迟消息
 */
@Slf4j
@Service
public class ActivityReminderServiceImpl implements IActivityReminderService {

    @Autowired
    private MQService mqService;

    @Autowired
    private VenueActivityParticipantMapper participantMapper;

    @Autowired
    private VenueActivityMapper activityMapper;

    /**
     * 活动开始前多久提醒（单位：秒）
     */
    @Value("${activity.reminder.advance.seconds:7200}")
    private int reminderAdvanceSeconds;

    @Override
    public void sendActivityReminderMessage(Long userId, Long activityId, Long orderNo) {
        if (activityId == null) {
            log.warn("[活动提醒] 活动ID为空 - userId={}", userId);
            return;
        }

        try {
            // 查询参与者记录（只查询未取消的）
            VenueActivityParticipant participant = participantMapper.selectOne(
                    Wrappers.lambdaQuery(VenueActivityParticipant.class)
                            .eq(VenueActivityParticipant::getActivityId, activityId)
                            .eq(VenueActivityParticipant::getUserId, userId)
                            .eq(VenueActivityParticipant::getStatus, VenueActivityParticipantStatusEnum.ACTIVE.getValue())
            );

            if (participant == null) {
                log.warn("[活动提醒] 未找到参与者记录 - userId={}, activityId={}", userId, activityId);
                return;
            }

            // 查询活动信息
            VenueActivity activity = activityMapper.selectById(activityId);
            if (activity == null || VenueActivityStatusEnum.CANCELLED.getValue().equals(activity.getStatus())) {
                log.warn("[活动提醒] 活动不存在 - activityId={}", activityId);
                return;
            }

            // 计算延迟时间（活动开始时间 - 提前提醒时间）
            LocalDateTime activityStartTime = LocalDateTime.of(activity.getActivityDate(), activity.getStartTime());
            LocalDateTime now = LocalDateTime.now();
            long delaySeconds = ChronoUnit.SECONDS.between(now, activityStartTime) - reminderAdvanceSeconds;

            if (delaySeconds <= 0) {
                log.warn("[活动提醒] 活动即将开始或已开始，不发送延迟消息 - activityId={}, delaySeconds={}",
                        activityId, delaySeconds);
                return;
            }

            // 构建延迟消息
            ActivityReminderMessage message = ActivityReminderMessage.builder()
                    .userId(userId)
                    .participantId(participant.getParticipantId())
                    .registrationTime(participant.getCreatedAt())
                    .orderNo(orderNo)
                    .build();

            // 发送延迟消息
            mqService.sendDelay(
                    VenueMQConstants.EXCHANGE_TOPIC_ACTIVITY_BOOKING_REMINDER,
                    VenueMQConstants.ROUTING_ACTIVITY_BOOKING_REMINDER,
                    message,
                    (int) delaySeconds
            );

            log.info("[活动提醒] 延迟消息已发送 - userId={}, activityId={}, orderNo={}, activityName={}, 延迟{}秒",
                    userId, activityId, orderNo, activity.getActivityName(), delaySeconds);

        } catch (Exception e) {
            log.error("[活动提醒] 发送延迟消息失败 - userId={}, activityId={}, orderNo={}", userId, activityId, orderNo, e);
        }
    }
}
