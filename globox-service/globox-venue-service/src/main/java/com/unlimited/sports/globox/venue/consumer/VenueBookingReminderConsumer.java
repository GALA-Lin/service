package com.unlimited.sports.globox.venue.consumer;

import com.rabbitmq.client.Channel;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.VenueMQConstants;
import com.unlimited.sports.globox.common.enums.notification.NotificationEventEnum;
import com.unlimited.sports.globox.common.enums.order.SellerTypeEnum;
import com.unlimited.sports.globox.common.message.venue.VenueBookingReminderMessage;
import com.unlimited.sports.globox.common.utils.NotificationSender;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订场即将开始提醒消费者
 * 消费延迟消息，检查槽位状态后发送通知
 * 流程：
 * 1. 接收延迟消息（只包含userId和recordIds）
 * 2. 查询完整的槽位、场地、场馆信息
 * 3. 检查槽位记录状态
 * 4. 如果槽位仍被占用（LOCKED_IN），发送通知到notification-service
 * 5. 如果槽位已释放，跳过通知
 */
@Slf4j
@Component
@RabbitListener(queues = VenueMQConstants.QUEUE_VENUE_BOOKING_REMINDER, concurrency = "3-5")
public class VenueBookingReminderConsumer {

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private NotificationSender notificationSender;

    /**
     * 处理订场提醒延迟消息
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            finalExchange = VenueMQConstants.EXCHANGE_VENUE_BOOKING_REMINDER_FINAL_DLX,
            finalRoutingKey = VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER_FINAL
    )
    public void onMessage(VenueBookingReminderMessage message, Channel channel, Message amqpMessage) {
        Long userId = message.getUserId();
        List<Long> recordIds = message.getRecordIds();
        LocalDateTime occupyTime = message.getOccupyTime();
        Long orderNo = message.getOrderNo();

        log.info("[订场提醒] 接收到延迟消息 - userId={}, recordIds={}, occupyTime={}, orderNo={}", userId, recordIds, occupyTime, orderNo);

        // 校验参数
        if (recordIds == null || recordIds.isEmpty()) {
            log.warn("[订场提醒] 槽位记录ID列表为空 - userId={}", userId);
            return;
        }

        if (occupyTime == null) {
            log.warn("[订场提醒] 槽位占用时间为空 - userId={}, recordIds={}", userId, recordIds);
            return;
        }

        try {
            // 通过SQL查询验证：recordIds匹配、状态为LOCKED_IN、userId匹配、updateTime匹配
            // 这样可以避免槽位释放后重新被占用导致通知错误
            List<VenueBookingSlotRecord> records = slotRecordMapper.selectList(
                    Wrappers.lambdaQuery(VenueBookingSlotRecord.class)
                            .in(VenueBookingSlotRecord::getBookingSlotRecordId, recordIds)
                            .eq(VenueBookingSlotRecord::getStatus, BookingSlotStatus.LOCKED_IN.getValue())
                            .eq(VenueBookingSlotRecord::getOperatorId, userId)
                            .eq(VenueBookingSlotRecord::getUpdatedAt, occupyTime)
            );

            // 验证查询结果：数量必须匹配，且所有recordId都在其中
            if (records.size() != recordIds.size()) {
                log.info("[订场提醒] 槽位已释放或被重新占用，跳过通知 - userId={}, recordIds={}, 查询到{}条",
                        userId, recordIds, records.size());
                return;
            }

            // 验证所有recordId都被查询到
            boolean allIdsMatch = recordIds.stream()
                    .allMatch(id -> records.stream()
                            .anyMatch(r -> r.getBookingSlotRecordId().equals(id)));

            if (!allIdsMatch) {
                log.info("[订场提醒] 部分槽位已释放或被重新占用，跳过通知 - userId={}, recordIds={}",
                        userId, recordIds);
                return;
            }

            // 查询槽位模板信息
            List<Long> templateIds = records.stream()
                    .map(VenueBookingSlotRecord::getSlotTemplateId)
                    .collect(Collectors.toList());
            List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(templateIds);
            Map<Long, VenueBookingSlotTemplate> templateMap = templates.stream()
                    .collect(Collectors.toMap(
                            VenueBookingSlotTemplate::getBookingSlotTemplateId,
                            t -> t
                    ));

            // 查询场地信息
            Set<Long> courtIds = templates.stream()
                    .map(VenueBookingSlotTemplate::getCourtId)
                    .collect(Collectors.toSet());
            List<Court> courts = courtMapper.selectBatchIds(courtIds);
            Map<Long, Court> courtMap = courts.stream()
                    .collect(Collectors.toMap(Court::getCourtId, c -> c));

            // 查询场馆信息
            Set<Long> venueIds = courts.stream()
                    .map(Court::getVenueId)
                    .collect(Collectors.toSet());
            List<Venue> venues = venueMapper.selectBatchIds(venueIds);
            Map<Long, Venue> venueMap = venues.stream()
                    .collect(Collectors.toMap(Venue::getVenueId, v -> v));

            // 获取第一个记录的信息（用于构建通知）
            VenueBookingSlotRecord firstRecord = records.get(0);
            VenueBookingSlotTemplate firstTemplate = templateMap.get(firstRecord.getSlotTemplateId());
            Court court = courtMap.get(firstTemplate.getCourtId());
            Venue venue = venueMap.get(court.getVenueId());

            // 计算时间范围
            VenueBookingSlotRecord lastRecord = records.get(records.size() - 1);
            VenueBookingSlotTemplate lastTemplate = templateMap.get(lastRecord.getSlotTemplateId());

            // 使用通用工具发送通知
            notificationSender.sendNotification(
                    userId,
                    NotificationEventEnum.VENUE_BOOKING_REMINDER,
                    recordIds.get(0),
                    NotificationSender.createCustomData()
                            .put("venueId", venue.getVenueId())
                            .put("venueName", venue.getName())
                            .put("courtId", court.getCourtId())
                            .put("courtName", court.getName())
                            .put("startTime", firstTemplate.getStartTime())
                            .put("endTime", lastTemplate.getEndTime())
                            .put("orderNo", orderNo)
                            .put("sellerType", SellerTypeEnum.VENUE.getCode())
                            .build()
            );

            log.info("[订场提醒] 通知发送成功 - userId={}, venueName={}, courtName={}, 时间段={}-{}",
                    userId, venue.getName(), court.getName(),
                    firstTemplate.getStartTime(), lastTemplate.getEndTime());

        } catch (Exception e) {
            log.error("[订场提醒] 处理失败 - userId={}, recordIds={}", userId, recordIds, e);
            throw e;
        }
    }
}
