package com.unlimited.sports.globox.coach.customer;

import com.rabbitmq.client.Channel;
import com.unlimited.sports.globox.coach.service.impl.CoachSlotServiceImpl;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;

import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * @since 2026/1/9 15:47
 * 教练订单确认
 */
@Slf4j
@Component
public class CoachUnlockCustomer {

    @Autowired
    private CoachSlotServiceImpl coachSlotService;

    @RabbitListener(queues = OrderMQConstants.QUEUE_ORDER_UNLOCK_COACH_SLOT_COACH)
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_CONFIRM_NOTIFY_MERCHANT_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_CONFIRM_NOTIFY_MERCHANT_FINAL
    )
    public void onUnlockSlotsMessage(
            UnlockSlotMessage message,
            Channel channel,
            Message amqpMessage) throws IOException {

        List<Long> recordIds = message.getRecordIds();
        Long userId = message.getUserId();

        log.info("[教练解锁槽位] 收到解锁请求消息 - userId: {}, recordIds: {}, bookingDate: {}",
                userId, recordIds, message.getBookingDate());

        try {
            // 参数验证
            if (recordIds == null || recordIds.isEmpty()) {
                log.warn("[教练解锁槽位] recordIds为空，跳过处理");
                return;
            }

            if (userId == null) {
                log.warn("[教练解锁槽位] userId为空，跳过处理");
                return;
            }

            // 批量解锁时段
            int unlockedCount = coachSlotService.batchUnlockSlots(recordIds, userId);

            log.info("[教练解锁槽位] 批量解锁成功 - userId: {}, 解锁数量: {}/{}",
                    userId, unlockedCount, recordIds.size());

        } catch (Exception e) {
            log.error("[教练解锁槽位] 处理失败 - userId: {}, recordIds: {}",
                    userId, recordIds, e);
            throw e;
        }
    }
}
