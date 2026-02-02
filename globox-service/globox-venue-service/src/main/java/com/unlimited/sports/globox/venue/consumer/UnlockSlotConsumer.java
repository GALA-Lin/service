package com.unlimited.sports.globox.venue.consumer;

import com.rabbitmq.client.Channel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unlimited.sports.globox.common.aop.RabbitRetryable;
import com.unlimited.sports.globox.common.constants.OrderMQConstants;
import com.unlimited.sports.globox.common.enums.governance.MQBizTypeEnum;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.message.order.UnlockSlotMessage;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.enums.VenueTypeEnum;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import com.unlimited.sports.globox.model.venue.entity.venues.*;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import com.unlimited.sports.globox.model.venue.enums.BookingSlotStatus;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapter;
import com.unlimited.sports.globox.venue.adapter.ThirdPartyPlatformAdapterFactory;
import com.unlimited.sports.globox.venue.adapter.dto.SlotLockRequest;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotRecordMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueBookingSlotTemplateMapper;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.venue.mapper.VenueThirdPartyConfigMapper;
import com.unlimited.sports.globox.venue.mapper.ThirdPartyPlatformMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Autowired
    private VenueBookingSlotTemplateMapper slotTemplateMapper;

    @Autowired
    private CourtMapper courtMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private VenueThirdPartyConfigMapper venueThirdPartyConfigMapper;

    @Autowired
    private ThirdPartyPlatformMapper thirdPartyPlatformMapper;

    @Autowired
    private ThirdPartyPlatformAdapterFactory adapterFactory;

    /**
     * 处理解锁槽位消息
     * MQ自动确认搭配自己的消费失败重试
     */
    @RabbitHandler
    @Transactional(rollbackFor = Exception.class)
    @RabbitRetryable(
            finalExchange = OrderMQConstants.EXCHANGE_ORDER_UNLOCK_SLOT_FINAL_DLX,
            finalRoutingKey = OrderMQConstants.ROUTING_ORDER_UNLOCK_SLOT_FINAL,
            bizKey = "#message.orderNo",
            bizType = MQBizTypeEnum.UNLOCK_SLOT_MERCHANT
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

        // 处理活动解锁（recordIds 是 participantIds）
        if (message.isActivity()) {
            handleActivityUnlock(userId, recordIds);
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

        Long venueId = records.get(0).getVenueId();
        Venue venue = venueMapper.selectById(venueId);
        if(venue == null) {
            log.error("检查away获取到的场馆信息为空,消息{},venueId{}",message,venueId);
            return;
        }
        if(VenueTypeEnum.AWAY.getCode().equals(venue.getVenueType())) {
            // 处理Away球场的解锁
            handleAwayUnlock(records,venue);
        }
    }

    /**
     * 处理Away球场的解锁
     * 1. 检查所有records是否都有thirdPartyBookingId（必须全有或全无）
     * 2. 如果都有，说明是Away订单，调用第三方平台解锁
     *
     * @param records 槽位记录列表
     */
    private void handleAwayUnlock(List<VenueBookingSlotRecord> records,Venue venue) {
        // 统计有thirdPartyBookingId的记录数量
        long recordsWithThirdPartyIdCount = records.stream()
                .filter(record -> record.getThirdPartyBookingId() != null
                        && !record.getThirdPartyBookingId().isEmpty())
                .count();

        // 检查一致性：要么全有，要么全无
        if (recordsWithThirdPartyIdCount == 0 ||  recordsWithThirdPartyIdCount < records.size()) {
            log.error("[Away解锁] 数据不一致：部分记录有thirdPartyBookingId，部分没有 - 总数: {}, 有thirdPartyId: {},需要人工干预解锁",
                    records.size(), recordsWithThirdPartyIdCount);
            // todo 人工干预解锁.如发邮件/插入记录表
            throw new GloboxApplicationException(VenueCode.VENUE_CAN_NOT_UNLOCK);
        }
        Long venueId = venue.getVenueId();
        List<Long> templateIds = records.stream()
                .map(VenueBookingSlotRecord::getSlotTemplateId)
                .toList();
        List<VenueBookingSlotTemplate> templates = slotTemplateMapper.selectBatchIds(templateIds);
        List<Long> courtIds = templates.stream().map(VenueBookingSlotTemplate::getCourtId).toList();
        List<Court> courts = courtMapper.selectByIds(courtIds);
        // 查询第三方平台配置
        VenueThirdPartyConfig config = venueThirdPartyConfigMapper.selectOne(
                new LambdaQueryWrapper<VenueThirdPartyConfig>()
                        .eq(VenueThirdPartyConfig::getVenueId, venueId)
        );
        if (config == null) {
            log.error("[Away解锁] Away球场未配置第三方平台 - venueId: {}", venueId);
            throw new GloboxApplicationException("Away解锁失败：未配置第三方平台");
        }
        // 查询平台信息，获取platformCode
        ThirdPartyPlatform platform = thirdPartyPlatformMapper.selectById(config.getThirdPartyPlatformId());
        if (platform == null) {
            log.error("[Away解锁] 第三方平台不存在 - platformId: {}", config.getThirdPartyPlatformId());
            throw new RuntimeException("Away解锁失败：第三方平台不存在");
        }

        String platformCode = platform.getPlatformCode();
        log.info("[Away解锁] 平台信息 - venueId: {}, platformCode: {}", venueId, platformCode);

        // 构建场地ID映射表（courtId -> Court）
        Map<Long, Court> courtMap = courts.stream()
                .collect(Collectors.toMap(Court::getCourtId, court -> court));

        // 构建模板映射表（templateId -> template）
        Map<Long, VenueBookingSlotTemplate> templateMap = templates.stream()
                .collect(Collectors.toMap(VenueBookingSlotTemplate::getBookingSlotTemplateId, template -> template));

        // 获取预订日期（假设所有record的日期相同）
        LocalDate bookingDate = records.get(0).getBookingDate().toLocalDate();

        // 构建解锁请求列表（包含第三方预订ID用于解锁）
        List<SlotLockRequest> unlockRequests = records.stream()
                .map(record -> {
                    VenueBookingSlotTemplate template = templateMap.get(record.getSlotTemplateId());
                    if (template == null) {
                        log.error("[Away解锁] 未找到模板 - templateId: {}", record.getSlotTemplateId());
                        return null;
                    }
                    Court court = courtMap.get(template.getCourtId());
                    if (court == null || court.getThirdPartyCourtId() == null) {
                        log.error("[Away解锁] 场地未配置第三方ID - courtId: {}", template.getCourtId());
                        return null;
                    }
                    return SlotLockRequest.builder()
                            .thirdPartyCourtId(court.getThirdPartyCourtId())
                            .startTime(template.getStartTime().toString())
                            .endTime(template.getEndTime().toString())
                            .date(bookingDate)
                            .thirdPartyBookingId(record.getThirdPartyBookingId())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (unlockRequests.isEmpty()) {
            log.error("[Away解锁] 无有效的解锁请求");
            throw new GloboxApplicationException("Away解锁失败：无有效的解锁请求");
        }

        // 根据platformCode获取适配器并调用解锁接口
        ThirdPartyPlatformAdapter adapter = adapterFactory.getAdapter(platformCode);
        log.info("[Away解锁] 开始调用第三方平台解锁 - venueId: {}, 槽位数: {}",
                venueId, unlockRequests.size());

        boolean unlockResult = adapter.unlockSlots(config, unlockRequests);

        if (!unlockResult) {
            log.error("[Away解锁] 第三方平台解锁失败 - venueId: {}, platformCode: {}, 解锁槽位数: {}",
                    venueId, platformCode, unlockRequests.size());
            throw new RuntimeException("Away解锁失败：第三方平台解锁返回失败");
        }

        log.info("[Away解锁] 第三方平台解锁成功 - venueId: {}, platformCode: {}, 解锁槽位数: {}",
                venueId, platformCode, unlockRequests.size());
    }

    /**
     * 处理活动参与者解绑
     * 核心逻辑：
     * 1. 根据参与记录ID（participantId）批量查询有效的参与记录
     * 2. 校验所有参与记录是否属于同一个活动
     * 3. 将这些记录标记为已取消（status=CANCELLED）
     * 4. 原子性递减活动人数（支持部分删除）
     *
     * @param userId 用户ID
     * @param participantIds 要删除的参与记录ID列表（从recordIds传入）
     */
    private void handleActivityUnlock(Long userId, List<Long> participantIds) {
        log.info("[活动解绑] 开始处理活动参与者解绑 - userId={}, participantIds={}",
                userId, participantIds);

        if (participantIds == null || participantIds.isEmpty()) {
            log.warn("[活动解绑] 参与记录ID列表为空 - userId={}", userId);
            return;
        }

        // 查询这些参与记录中有效的（status=ACTIVE）
        List<VenueActivityParticipant> participants = activityParticipantMapper.selectList(
                new LambdaQueryWrapper<VenueActivityParticipant>()
                        .in(VenueActivityParticipant::getParticipantId, participantIds)
                        .eq(VenueActivityParticipant::getStatus, VenueActivityParticipantStatusEnum.ACTIVE.getValue())
        );

        if (participants.isEmpty()) {
            log.warn("[活动解绑] 未找到有效的参与记录 - userId={}, participantIds={}", userId, participantIds);
            return;
        }

        // 校验所有参与记录是否属于同一个活动
        Long activityId = participants.get(0).getActivityId();
        boolean allSameActivity = participants.stream()
                .allMatch(p -> p.getActivityId().equals(activityId));

        if (!allSameActivity) {
            log.warn("[活动解绑] 参与记录属于不同活动，禁止跨活动删除 - userId={}, participantIds={}", userId, participantIds);
            return;
        }

        log.info("[活动解绑] 找到 {} 条有效参与记录 - activityId={}, userId={}", participants.size(), activityId, userId);

        // 批量更新为已取消状态
        int cancelCount = activityParticipantMapper.update(null,
                new LambdaUpdateWrapper<VenueActivityParticipant>()
                        .set(VenueActivityParticipant::getStatus, VenueActivityParticipantStatusEnum.CANCELLED.getValue())
                        .in(VenueActivityParticipant::getParticipantId, participantIds)
                        .eq(VenueActivityParticipant::getStatus, VenueActivityParticipantStatusEnum.ACTIVE.getValue())
        );

        if (cancelCount == 0) {
            log.warn("[活动解绑] 取消失败 - 记录已被修改 - userId={}, participantIds={}", userId, participantIds);
            return;
        }

        log.info("[活动解绑] 成功取消参与记录 - activityId={}, userId={}, participantIds={}, 取消数量={}",
                activityId, userId, participantIds, cancelCount);

        // 原子性递减活动人数（数据库级别保证原子性）
        int decrementCount = venueActivityMapper.update(null,
                new LambdaUpdateWrapper<VenueActivity>()
                        .setSql("current_participants = current_participants - " + cancelCount)
                        .eq(VenueActivity::getActivityId, activityId)
                        .apply("current_participants >= {0}", cancelCount)
        );

        if (decrementCount == 0) {
            log.warn("[活动解绑] 递减失败 - 活动不存在或人数不足 - activityId={}, 需要递减={}",
                    activityId, cancelCount);
            return;
        }

        log.info("[活动解绑] 解绑成功 - activityId={}, userId={}, participantIds={}, 递减人数={}",
                activityId, userId, participantIds, cancelCount);
    }
}
