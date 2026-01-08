package com.unlimited.sports.globox.venue.service.impl;

import com.unlimited.sports.globox.common.constants.VenueMQConstants;
import com.unlimited.sports.globox.common.message.venue.VenueBookingReminderMessage;
import com.unlimited.sports.globox.common.service.MQService;
import com.unlimited.sports.globox.common.utils.CollectionUtils;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.venue.service.IVenueBookingReminderService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订场提醒服务实现
 * 只负责分组和发送延迟消息，不查询详细的场地、场馆信息
 */
@Slf4j
@Service
public class VenueBookingReminderServiceImpl implements IVenueBookingReminderService {

    @Autowired
    private MQService mqService;

    @Autowired
    private VenueBookingSlotRecordMapper slotRecordMapper;

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    /**
     * 订场开始前多久提醒（单位：秒）
     */
    @Value("${venue.reminder.advance.seconds:7200}")
    private int reminderAdvanceSeconds;

    @Override
    public void sendBookingReminderMessages(Long userId, List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            log.warn("[订场提醒] 槽位记录ID列表为空 - userId={}", userId);
            return;
        }

        try {
            // 查询槽位记录
            List<VenueBookingSlotRecord> records = slotRecordMapper.selectBatchIds(recordIds);
            if (records.isEmpty()) {
                log.warn("[订场提醒] 未找到槽位记录 - userId={}, recordIds={}", userId, recordIds);
                return;
            }
            // 查询槽位模板信息
            Map<Long, VenueBookingSlotTemplate> templateMap = getTemplateMap(records);
            // 按照场地分组，然后按连续时间段分组，构建并发送延迟消息
            List<ReminderMessageToBeSent> messagesToSend = records.stream()
                    // 过滤掉无效的模板
                    .filter(record -> {
                        VenueBookingSlotTemplate template = templateMap.get(record.getSlotTemplateId());
                        return template != null && template.getCourtId() != null;
                    })
                    // 按场地分组
                    .collect(Collectors.groupingBy(
                            record -> templateMap.get(record.getSlotTemplateId()).getCourtId()
                    ))
                    .values().stream()
                    .flatMap(courtRecords -> {
                        // 按照时间段排序
                        List<VenueBookingSlotRecord> sortedRecords = courtRecords.stream()
                                .sorted(Comparator.comparing(
                                        r -> templateMap.get(r.getSlotTemplateId()).getStartTime()
                                ))
                                .collect(Collectors.toList());

                        // 按照连续时间段分组
                        List<List<VenueBookingSlotRecord>> continuousGroups = CollectionUtils.splitByContinuity(
                                sortedRecords,
                                (prev, curr) -> {
                                    VenueBookingSlotTemplate prevTemplate = templateMap.get(prev.getSlotTemplateId());
                                    VenueBookingSlotTemplate currTemplate = templateMap.get(curr.getSlotTemplateId());
                                    return isContinuous(prevTemplate, currTemplate);
                                }
                        );

                        // 为每个连续时间段构建消息
                        return continuousGroups.stream()
                                .map(group -> buildReminderMessage(userId, group, templateMap))
                                .filter(Objects::nonNull);
                    })
                    .toList();

            // 发送所有延迟消息
            messagesToSend.forEach(this::sendReminderMessage);

            log.info("[订场提醒] 延迟消息发送完成 - userId={}, 总槽数={}, 发送消息数={}",
                    userId, recordIds.size(), messagesToSend.size());

        } catch (Exception e) {
            // 发送失败不影响其他业务，只记录日志
            log.error("[订场提醒] 发送延迟消息失败 - userId={}, recordIds={}", userId, recordIds, e);
        }
    }

    /**
     * 查询槽位模板信息并构建Map
     */
    private Map<Long, VenueBookingSlotTemplate> getTemplateMap(List<VenueBookingSlotRecord> records) {
        List<Long> templateIds = records.stream()
                .map(VenueBookingSlotRecord::getSlotTemplateId)
                .distinct()
                .collect(Collectors.toList());

        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(templateIds);

        return templates.stream()
                .collect(Collectors.toMap(
                        VenueBookingSlotTemplate::getBookingSlotTemplateId,
                        Function.identity()
                ));
    }

    /**
     * 判断两个时间段是否连续
     */
    private boolean isContinuous(VenueBookingSlotTemplate t1, VenueBookingSlotTemplate t2) {
        return Optional.ofNullable(t1)
                .flatMap(template1 -> Optional.ofNullable(t2)
                        .filter(template2 -> template1.getEndTime().equals(template2.getStartTime())))
                .isPresent();
    }

    /**
     * 构建订场提醒消息
     */
    private ReminderMessageToBeSent buildReminderMessage(
            Long userId,
            List<VenueBookingSlotRecord> records,
            Map<Long, VenueBookingSlotTemplate> templateMap) {

        if (records.isEmpty()) {
            return null;
        }

        return Optional.of(records)
                .map(list -> {
                    VenueBookingSlotRecord firstRecord = list.get(0);
                    VenueBookingSlotRecord lastRecord = list.get(list.size() - 1);
                    VenueBookingSlotTemplate firstTemplate = templateMap.get(firstRecord.getSlotTemplateId());
                    VenueBookingSlotTemplate lastTemplate = templateMap.get(lastRecord.getSlotTemplateId());

                    if (firstTemplate == null || lastTemplate == null) {
                        return null;
                    }

                    // 获取槽位占用时间（用于验证）
                    LocalDateTime occupyTime = firstRecord.getUpdatedAt();

                    // 计算延迟时间
                    LocalDateTime bookingStartDateTime = LocalDateTime.of(
                            firstRecord.getBookingDate().toLocalDate(),
                            firstTemplate.getStartTime()
                    );
                    LocalDateTime now = LocalDateTime.now();
                    long secondsUntilStart = ChronoUnit.SECONDS.between(now, bookingStartDateTime);

                    log.info("[订场提醒] 时间计算 - now={}, bookingDate={}, startTime={}, bookingStartDateTime={}, secondsUntilStart={}秒, reminderAdvanceSeconds={}秒",
                            now,
                            firstRecord.getBookingDate().toLocalDate(),
                            firstTemplate.getStartTime(),
                            bookingStartDateTime,
                            secondsUntilStart,
                            reminderAdvanceSeconds);

                    long delaySeconds = secondsUntilStart - reminderAdvanceSeconds;

                    if (delaySeconds <= 0) {
                        log.info("[订场提醒] 距离开始时间太近，跳过提醒 - userId={}, 时间段={}-{}",
                                userId,
                                firstTemplate.getStartTime(), lastTemplate.getEndTime());
                        return null;
                    }
                    // 构建recordIds列表
                    List<Long> groupedRecordIds = list.stream()
                            .map(VenueBookingSlotRecord::getBookingSlotRecordId)
                            .collect(Collectors.toList());

                    // 构建延迟消息
                    VenueBookingReminderMessage message = VenueBookingReminderMessage.builder()
                            .userId(userId)
                            .recordIds(groupedRecordIds)
                            .occupyTime(occupyTime)
                            .build();

                    return new ReminderMessageToBeSent(message, (int) delaySeconds,
                            firstTemplate.getStartTime(), lastTemplate.getEndTime());
                })
                .orElse(null);
    }

    /**
     * 发送单条订场提醒延迟消息
     */
    private void sendReminderMessage(ReminderMessageToBeSent msg) {
        try {
            mqService.sendDelay(
                    VenueMQConstants.EXCHANGE_TOPIC_VENUE_BOOKING_REMINDER,
                    VenueMQConstants.ROUTING_VENUE_BOOKING_REMINDER,
                    msg.getMessage(),
                    msg.getDelaySeconds()
            );

            log.info("[订场提醒] 延迟消息已发送 - userId={}, 时间段={}-{}, 延迟{}秒",
                    msg.getMessage().getUserId(),
                    msg.getStartTime(),
                    msg.getEndTime(),
                    msg.getDelaySeconds());

        } catch (Exception e) {
            log.error("[订场提醒] 发送延迟消息失败 - 时间段={}-{}",
                    msg.getStartTime(), msg.getEndTime(), e);
        }
    }

    /**
     * 待发送的提醒消息（内部使用）
     */
    @Data
    @AllArgsConstructor
    private static class ReminderMessageToBeSent {
        private VenueBookingReminderMessage message;
        private int delaySeconds;
        private Object startTime;
        private Object endTime;
    }
}
