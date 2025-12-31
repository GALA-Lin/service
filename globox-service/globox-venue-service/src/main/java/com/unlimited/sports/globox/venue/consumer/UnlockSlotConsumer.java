package com.unlimited.sports.globox.venue.consumer;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.unlimited.sports.globox.common.constants.MQConstants;
import com.unlimited.sports.globox.common.message.UnlockSlotMessage;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import lombok.extern.slf4j.Slf4j;
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
@RabbitListener(queues = MQConstants.QUEUE_ORDER_UNLOCK_SLOT_MERCHANT)
public class UnlockSlotConsumer {

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    /**
     * 处理解锁槽位消息
     * MQ自动确认搭配自己的消费失败重试
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(UnlockSlotMessage message) {

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

        UpdateWrapper<VenueBookingSlotRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("booking_slot_record_id", lockedRecordIds)
                .eq("status", BookingSlotStatus.LOCKED_IN.getValue())  // 只能解锁LOCKED_IN状态的槽位
                .eq("operator_id", userId);  // 只能解锁自己占用的槽位

        int updatedCount = slotRecordMapper.update(updateRecord, updateWrapper);

        if (updatedCount == 0) {
            log.warn("[槽位解锁] 解锁失败 - 槽位已被修改或不属于该用户 userId={}, recordIds={}, bookingDate={}",
                    userId, lockedRecordIds, bookingDate);
            return;
        }

        log.info("[槽位解锁] 解锁成功 - userId={}, 总槽位数={}, 解锁数={}, 更新数={}",
                userId, recordIds.size(), lockedRecordIds.size(), updatedCount);
    }
}
