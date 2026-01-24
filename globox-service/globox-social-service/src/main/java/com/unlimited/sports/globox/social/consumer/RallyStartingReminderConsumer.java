package com.unlimited.sports.globox.social.consumer;

import com.rabbitmq.client.Channel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.RallyMQConstants;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.message.social.RallyStartingReminderMessage;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.model.social.entity.RallyParticipant;
import com.unlimited.sports.globox.model.social.entity.RallyPosts;
import com.unlimited.sports.globox.model.social.entity.RallyPostsStatusEnum;
import com.unlimited.sports.globox.social.mapper.RallyParticipantMapper;
import com.unlimited.sports.globox.social.mapper.RallyPostsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 约球即将开始提醒消费者
 * 消费延迟消息，检查约球状态后发送通知给发起人和参与者
 * 流程：
 * 1. 接收延迟消息（只包含rallyId）
 * 2. 查询约球信息，检查约球是否还存在
 * 3. 如果约球存在，发送通知给发起人："您发起的约球 xxx 于 时间 开始，请按时参加"
 * 4. 获取所有有效参与者，发送通知给每个参与者："您参与的约球 xxx 于 时间 开始，请按时参加"
 * 5. 如果约球已删除或取消，跳过通知
 */
@Slf4j
@Component
@RabbitListener(queues = RallyMQConstants.QUEUE_RALLY_STARTING_REMINDER, concurrency = "3-5")
public class RallyStartingReminderConsumer {

    @Autowired
    private RallyPostsMapper rallyPostsMapper;

    @Autowired
    private RallyParticipantMapper rallyParticipantMapper;

    @Autowired
    private NotificationSender notificationSender;

    /**
     * 处理约球提醒延迟消息
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(RallyStartingReminderMessage message, Channel channel, Message amqpMessage) throws IOException {
        Long rallyId = message.getRallyId();

        log.info("[约球提醒] 接收到延迟消息 - rallyId={}", rallyId);

        // 校验参数
        if (rallyId == null) {
            log.warn("[约球提醒] 约球ID为空");
            return;
        }

        try {
            // 查询约球信息
            RallyPosts rallyPosts = rallyPostsMapper.selectById(rallyId);
            if (rallyPosts == null) {
                log.info("[约球提醒] 约球不存在，跳过通知 - rallyId={}", rallyId);
                return;
            }

            // 检查约球状态（如果已取消，跳过通知）
            if (rallyPosts.getRallyStatus() == RallyPostsStatusEnum.CANCELLED.getCode()) {
                log.info("[约球提醒] 约球状态异常（已取消或已完成），跳过通知 - rallyId={}, status={}",
                        rallyId, rallyPosts.getRallyStatus());
                return;
            }

            // 1. 发送通知给发起人
            sendInitiatorReminder(rallyPosts);

            // 2. 发送通知给所有有效参与者
            sendParticipantReminders(rallyPosts);

            log.info("[约球提醒] 通知发送完成 - rallyId={}, rallyTitle={}", rallyId, rallyPosts.getRallyTitle());

        } catch (Exception e) {
            log.error("[约球提醒] 处理失败 - rallyId={}", rallyId, e);
            throw e;
        } finally {
            long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
        }
    }

    /**
     * 发送提醒通知给约球发起人
     */
    private void sendInitiatorReminder(RallyPosts rallyPosts) {
        try {
            Map<String, Object> customData = new HashMap<>();
            customData.put("rallyId", rallyPosts.getRallyPostId());
            customData.put("rallyTitle", rallyPosts.getRallyTitle());
            customData.put("rallyDate", rallyPosts.getRallyEventDate());
            customData.put("rallyTime", rallyPosts.getRallyStartTime());
            customData.put("rallyStatus", rallyPosts.getRallyStatus());
            customData.put("rallyStatusDesc", RallyPostsStatusEnum.fromCode(rallyPosts.getRallyStatus()).getDescription());

            notificationSender.sendNotification(
                    rallyPosts.getInitiatorId(),
                    NotificationEventEnum.RALLY_STARTING_REMINDER_INITIATOR,
                    rallyPosts.getRallyPostId(),
                    customData
            );

            log.info("[约球提醒] 发起人提醒已发送 - rallyId={}, initiatorId={}, rallyTitle={}",
                    rallyPosts.getRallyPostId(), rallyPosts.getInitiatorId(), rallyPosts.getRallyTitle());
        } catch (Exception e) {
            log.error("[约球提醒] 发送发起人提醒失败 - rallyId={}", rallyPosts.getRallyPostId(), e);
            // 单个通知失败不影响其他通知继续发送
        }
    }

    /**
     * 发送提醒通知给所有有效参与者
     */
    private void sendParticipantReminders(RallyPosts rallyPosts) {
        try {
            // 查询约球的所有参与者（不包括发起人，因为发起人已单独处理）
            List<RallyParticipant> participants = rallyParticipantMapper.selectList(
                    Wrappers.<RallyParticipant>lambdaQuery()
                            .eq(RallyParticipant::getRallyPostId, rallyPosts.getRallyPostId())
                            .eq(RallyParticipant::getIsVoluntarilyCancel, 0) // 只发送给未主动取消的参与者
                            .ne(RallyParticipant::getParticipantId, rallyPosts.getInitiatorId()) // 排除发起人
            );

            if (participants.isEmpty()) {
                log.info("[约球提醒] 没有有效参与者 - rallyId={}", rallyPosts.getRallyPostId());
                return;
            }

            // 为每个参与者发送通知
            for (RallyParticipant participant : participants) {
                try {
                    Map<String, Object> customData = new HashMap<>();
                    customData.put("rallyId", rallyPosts.getRallyPostId());
                    customData.put("rallyTitle", rallyPosts.getRallyTitle());
                    customData.put("rallyDate", rallyPosts.getRallyEventDate());
                    customData.put("rallyTime", rallyPosts.getRallyStartTime());
                    customData.put("rallyStatus", rallyPosts.getRallyStatus());
                    customData.put("rallyStatusDesc", RallyPostsStatusEnum.fromCode(rallyPosts.getRallyStatus()).getDescription());

                    notificationSender.sendNotification(
                            participant.getParticipantId(),
                            NotificationEventEnum.RALLY_STARTING_REMINDER_PARTICIPANT,
                            rallyPosts.getRallyPostId(),
                            customData
                    );

                    log.info("[约球提醒] 参与者提醒已发送 - rallyId={}, participantId={}",
                            rallyPosts.getRallyPostId(), participant.getParticipantId());
                } catch (Exception e) {
                    log.error("[约球提醒] 发送参与者提醒失败 - rallyId={}, participantId={}, error={}",
                            rallyPosts.getRallyPostId(), participant.getParticipantId(), e.getMessage());
                    // 单个参与者通知失败不影响其他参与者通知继续发送
                }
            }

            log.info("[约球提醒] 参与者提醒发送完成 - rallyId={}, 参与者数量={}",
                    rallyPosts.getRallyPostId(), participants.size());
        } catch (Exception e) {
            log.error("[约球提醒] 发送参与者提醒失败 - rallyId={}", rallyPosts.getRallyPostId(), e);
        }
    }
}
