package com.unlimited.sports.globox.venue.consumer;

import com.rabbitmq.client.Channel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.venue.constants.ActivityParticipantConstants;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 槽位解锁消费者
 * 处理订单创建失败或取消时的槽位解锁逻辑
 */
@Slf4j
@Component
@RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_UNLOCK_SLOT_MERCHANT)
public class UnlockSlotConsumer {

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private VenueActivityParticipantMapper activityParticipantMapper;

    @Autowired
    private VenueActivityMapper venueActivityMapper;

    /**
     * 处理解锁槽位消息
     * MQ自动确认搭配自己的消费失败重试
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_UNLOCK_SLOT_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT_FINAL
    )
    public void onMessage(UnlockSlotMessage message, Channel channel, Message amqpMessage) {

        Long userId = message.getUserId();
        List<Long> recordIds = message.getRecordIds();
        LocalDate bookingDate = message.getBookingDate();

        log.info("[槽位解锁] 接收到消息 - userId={}, recordIds={}, bookingDate={}",
                userId, recordIds, bookingDate);

        // 校验参数
        if (recordIds == null || recordIds.isEmpty()) {
            log.warn("[槽位解锁] 槽位记录ID列表为空 - userId={}", userId);
            return;
        }

        // 处理活动解锁
        if (message.isActivity()) {
            handleActivityUnlock(recordIds.get(0), userId);
            return;
        }

        // 查询对应的槽位记录
        List<VenueBookingSlotRecord> records = slotRecordMapper.selectBatchIds(recordIds);

        if (records.isEmpty()) {
            log.warn("[槽位解锁] 未找到对应的槽位记录 - userId={}, recordIds={}, bookingDate={}",
                    userId, recordIds, bookingDate);
            return;
        }

        // 筛选需要解锁的记录（状态为LOCKED_IN的）
        List<Long> lockedRecordIds = records.stream()
                .filter(record -> record.getStatus() == BookingSlotStatus.LOCKED_IN.getValue())
                .map(VenueBookingSlotRecord::getBookingSlotRecordId)
                .collect(Collectors.toList());

        if (lockedRecordIds.isEmpty()) {
            log.info("[槽位解锁] 没有需要解锁的槽位 - userId={}, recordIds={}, bookingDate={}",
                    userId, recordIds, bookingDate);
            return;
        }

        // 原子性解锁：只有当前状态为LOCKED_IN且operator_id匹配时才能解锁
        // 防止并发占用和解锁的竞态条件
        VenueBookingSlotRecord updateRecord = new VenueBookingSlotRecord();
        updateRecord.setStatus(BookingSlotStatus.AVAILABLE.getValue());
        updateRecord.setOperatorId(null);
        updateRecord.setOperatorSource(null);

        int updatedCount = slotRecordMapper.update(updateRecord,
                new LambdaUpdateWrapper<VenueBookingSlotRecord>()
                        .in(VenueBookingSlotRecord::getBookingSlotRecordId, lockedRecordIds)
                        .eq(VenueBookingSlotRecord::getStatus, BookingSlotStatus.LOCKED_IN.getValue())  // 只能解锁LOCKED_IN状态的槽位
                        .eq(VenueBookingSlotRecord::getOperatorId, userId))  // 只能解锁自己占用的槽位
        ;

        if (updatedCount == 0) {
            log.warn("[槽位解锁] 解锁失败 - 槽位已被修改或不属于该用户 userId={}, recordIds={}, bookingDate={}",
                    userId, lockedRecordIds, bookingDate);
            return;
        }

        log.info("[槽位解锁] 解锁成功 - userId={}, 总槽位数={}, 解锁数={}, 更新数={}",
                userId, recordIds.size(), lockedRecordIds.size(), updatedCount);
    }

    /**
     * 处理活动参与者解绑 -
     * 核心逻辑：
     * 1. 软删除参与记录（基于activityId+userId+is_deleted=1，幂等操作）
     * 2. 检查软删除是否成功（影响行数==1时才进行步骤3）
     * 3. 原子性递减活动人数（数据库级别保证原子性）
     *
     * @param activityId 活动ID（从recordIds.get(0)传入）
     * @param userId 用户ID
     */
    private void handleActivityUnlock(Long activityId, Long userId) {
        log.info("[活动解绑] 开始处理活动参与者解绑 - activityId={}, userId={}", activityId, userId);

        if (activityId == null || activityId <= 0) {
            log.warn("[活动解绑] 活动ID无效 - activityId={}, userId={}", activityId, userId);
            return;
        }

        // 查询未取消的参与记录
        VenueActivityParticipant participant = activityParticipantMapper.selectOne(
                new LambdaQueryWrapper<VenueActivityParticipant>()
                        .eq(VenueActivityParticipant::getActivityId, activityId)
                        .eq(VenueActivityParticipant::getUserId, userId)
                        .eq(VenueActivityParticipant::getDeleteVersion, ActivityParticipantConstants.DELETE_VERSION_ACTIVE)
        );

        if (participant == null) {
            log.warn("[活动解绑] 未找到未取消的参与记录 - activityId={}, userId={}", activityId, userId);
            return;
        }

        // 更新为已取消，deleteVersion设为参与记录ID
        int deleteCount = activityParticipantMapper.update(null,
                new LambdaUpdateWrapper<VenueActivityParticipant>()
                        .set(VenueActivityParticipant::getDeleteVersion, participant.getParticipantId())
                        .eq(VenueActivityParticipant::getParticipantId, participant.getParticipantId())
                        .eq(VenueActivityParticipant::getDeleteVersion, ActivityParticipantConstants.DELETE_VERSION_ACTIVE)
        );

        if (deleteCount == 0) {
            log.warn("[活动解绑] 更新失败 - 记录已被修改 - activityId={}, userId={}", activityId, userId);
            return;
        }

        // 步骤2：原子性递减活动人数（数据库级别保证原子性）
        // UPDATE venue_activity SET current_participants = current_participants - 1 WHERE activity_id = ? AND current_participants > 0
        int decrementCount = venueActivityMapper.update(null,
                new LambdaUpdateWrapper<VenueActivity>()
                        .setSql("current_participants = current_participants - 1")
                        .eq(VenueActivity::getActivityId, activityId)
                        .gt(VenueActivity::getCurrentParticipants, 0)
        );

        if (decrementCount == 0) {
            log.warn("[活动解绑] 递减失败 - 活动不存在或人数已为0 - activityId={}, userId={}",
                    activityId, userId);
            return;
        }

        log.info("[活动解绑] 解绑成功 - activityId={}, userId={}, 人数已递减",
                activityId, userId);
    }
}
