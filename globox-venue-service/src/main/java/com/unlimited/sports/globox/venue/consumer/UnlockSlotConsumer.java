package com.unlimited.sports.globox.venue.consumer;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.common.constants.MQConstants;
import com.unlimited.sports.globox.common.message.UnlockSlotMessage;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 槽位解锁消费者
 * 处理订单创建失败或取消时的槽位解锁逻辑
 */
@Slf4j
@Component
@RabbitListener(queues = MQConstants.QUEUE_ORDER_UNLOCK_SLOT_MERCHANT)
public class UnlockSlotConsumer {

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    /**
     * 处理解锁槽位消息
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(
            UnlockSlotMessage message,
            Channel channel,
            Message amqpMessage) throws IOException {

        Long userId = message.getUserId();
        List<Long> slotIds = message.getRecordIds();
        LocalDate bookingDate = message.getBookingDate();

        log.info("[槽位解锁] 接收到消息 - userId={}, slotIds={}, bookingDate={}",
                userId, slotIds, bookingDate);

        try {
            // 校验参数
            if (slotIds == null || slotIds.isEmpty()) {
                log.warn("[槽位解锁] 槽位ID列表为空 - userId={}", userId);
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            //  查询对应的槽位记录
            List<VenueBookingSlotRecord> records = slotRecordMapper.selectByTemplateIdsAndDate(
                    slotIds,
                    bookingDate
            );

            if (records.isEmpty()) {
                log.warn("[槽位解锁] 未找到对应的槽位记录 - userId={}, slotIds={}, bookingDate={}",
                        userId, slotIds, bookingDate);
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 筛选需要解锁的记录（状态为LOCKED_IN的）
            List<Long> lockedRecordIds = records.stream()
                    .filter(record -> record.getStatus() == BookingSlotStatus.LOCKED_IN.getValue())
                    .map(VenueBookingSlotRecord::getBookingSlotRecordId)
                    .collect(Collectors.toList());

            if (lockedRecordIds.isEmpty()) {
                log.info("[槽位解锁] 没有需要解锁的槽位 - userId={}, slotIds={}, bookingDate={}",
                        userId, slotIds, bookingDate);
                channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 使用UpdateWrapper进行批量更新
            UpdateWrapper<VenueBookingSlotRecord> updateWrapper = new UpdateWrapper<>();
            updateWrapper.set("status", BookingSlotStatus.AVAILABLE.getValue())
                    .set("operator_id", null)
                    .set("operator_source", null)
                    .in("booking_slot_record_id", lockedRecordIds);

            int updatedCount = slotRecordMapper.update(null, updateWrapper);

            log.info("[槽位解锁] 解锁成功 - userId={}, 总槽位数={}, 解锁数={}, 更新数={}",
                    userId, slotIds.size(), lockedRecordIds.size(), updatedCount);

            //  ACK确认
            channel.basicAck(amqpMessage.getMessageProperties().getDeliveryTag(), false);

        } catch (Exception e) {
            log.error("[槽位解锁] 处理失败 - userId={}, slotIds={}, bookingDate={}",
                    userId, slotIds, bookingDate, e);
            // 失败重试
            channel.basicNack(
                    amqpMessage.getMessageProperties().getDeliveryTag(),
                    false,
                    true);
        }
    }
}
