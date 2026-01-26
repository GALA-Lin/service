package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.VenueCode;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipantStatusEnum;
import com.unlimited.sports.globox.model.venue.enums.VenueActivityStatusEnum;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import com.unlimited.sports.globox.venue.service.IVenueActivityParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.unlimited.sports.globox.common.utils.IdGenerator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 活动参与者 Service 实现
 */
@Service
@Slf4j
public class VenueActivityParticipantServiceImpl extends ServiceImpl<VenueActivityParticipantMapper, VenueActivityParticipant>
        implements IVenueActivityParticipantService {

    @Autowired
    private VenueActivityMapper activityMapper;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VenueActivityParticipant registerUserToActivity(Long activityId, Long userId) {
        if (activityId == null || userId == null) {
            throw new GloboxApplicationException(VenueCode.ACTIVITY_PARAM_INVALID);
        }

        // 查询活动详情
        VenueActivity activity = activityMapper.selectById(activityId);
        if (activity == null || VenueActivityStatusEnum.CANCELLED.getValue().equals(activity.getStatus())) {
            throw new GloboxApplicationException(VenueCode.ACTIVITY_NOT_EXIST);
        }

        // 检查是否已经报名（只检查未取消的记录）
        VenueActivityParticipant existing = this.getOne(
                new LambdaQueryWrapper<VenueActivityParticipant>()
                        .eq(VenueActivityParticipant::getActivityId, activityId)
                        .eq(VenueActivityParticipant::getUserId, userId)
                        .eq(VenueActivityParticipant::getStatus, VenueActivityParticipantStatusEnum.ACTIVE.getValue())
        );
        if (existing != null) {
            throw new GloboxApplicationException(VenueCode.ACTIVITY_ALREADY_REGISTERED);
        }

        // 原子操作：检查人数限制并增加参与人数
        // 使用 current_participants = current_participants + 1 和 WHERE 条件防止超卖
        int updated = 0;
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            // 有人数限制：使用 current_participants + 1 而不是常数值
            updated = activityMapper.update(null, new LambdaUpdateWrapper<VenueActivity>()
                    .setSql("current_participants = current_participants + 1")
                    .eq(VenueActivity::getActivityId, activityId)
                    .lt(VenueActivity::getCurrentParticipants, activity.getMaxParticipants())
            );
        } else {
            // 无人数限制，直接增加
            updated = activityMapper.update(null, new LambdaUpdateWrapper<VenueActivity>()
                    .setSql("current_participants = current_participants + 1")
                    .eq(VenueActivity::getActivityId, activityId)
            );
        }

        if (updated == 0) {
            log.warn("活动名额已满或不存在 - activityId: {}, userId: {}", activityId, userId);
            throw new GloboxApplicationException(VenueCode.ACTIVITY_FULL);
        }

        log.info("活动人数已增加 - activityId: {}, userId: {}", activityId, userId);

        VenueActivityParticipant participant = VenueActivityParticipant.builder()
                .activityId(activityId)
                .userId(userId)
                .status(VenueActivityParticipantStatusEnum.ACTIVE.getValue())
                .build();

        this.save(participant);
        log.info("用户报名成功 - activityId: {}, userId: {}, participantId: {}",
                activityId, userId, participant.getParticipantId());
        return participant;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public List<VenueActivityParticipant> registerMultipleParticipants(Long activityId, Long userId, Integer quantity, String phone) {
        if (activityId == null || userId == null || quantity == null || quantity <= 0) {
            throw new GloboxApplicationException(VenueCode.ACTIVITY_PARAM_INVALID);
        }

        // 查询活动详情
        VenueActivity activity = activityMapper.selectById(activityId);
        if (activity == null || VenueActivityStatusEnum.CANCELLED.getValue().equals(activity.getStatus())) {
            throw new GloboxApplicationException(VenueCode.ACTIVITY_NOT_EXIST);
        }

        // 检查该用户是否已经报名（未取消的记录）
        long existingCount = this.count(
                new LambdaQueryWrapper<VenueActivityParticipant>()
                        .eq(VenueActivityParticipant::getActivityId, activityId)
                        .eq(VenueActivityParticipant::getUserId, userId)
                        .eq(VenueActivityParticipant::getStatus, VenueActivityParticipantStatusEnum.ACTIVE.getValue())
        );

        if (existingCount > 0) {
            log.warn("用户已报名活动 - activityId: {}, userId: {}, 已报名数: {}",
                    activityId, userId, existingCount);
            throw new GloboxApplicationException(VenueCode.ACTIVITY_ALREADY_REGISTERED);
        }

        // 原子操作：一次性增加 quantity 个名额
        // 直接在数据库层判断：current_participants + quantity <= max_participants
        int updated = 0;
        if (activity.getMaxParticipants() != null && activity.getMaxParticipants() > 0) {
            // 有人数限制：使用数据库字段判断
            updated = activityMapper.update(null, new LambdaUpdateWrapper<VenueActivity>()
                    .setSql("current_participants = current_participants + " + quantity)
                    .eq(VenueActivity::getActivityId, activityId)
                    .apply("current_participants + {0} <= max_participants", quantity)
            );
        } else {
            // 无人数限制，直接增加
            updated = activityMapper.update(null, new LambdaUpdateWrapper<VenueActivity>()
                    .setSql("current_participants = current_participants + " + quantity)
                    .eq(VenueActivity::getActivityId, activityId)
            );
        }

        if (updated == 0) {
            log.warn("活动名额不足 - activityId: {}, userId: {}, 需要名额: {}",
                    activityId, userId, quantity);
            throw new GloboxApplicationException(VenueCode.ACTIVITY_NO_SLOTS);
        }

        log.info("活动人数已增加 - activityId: {}, userId: {}, 增加数量: {}", activityId, userId, quantity);

        // 生成唯一的batch_id用于追踪同一批次的所有报名（使用雪花算法）
        String batchId = String.valueOf(idGenerator.nextId());

        // 批量插入参与记录，所有记录使用相同的batch_id和phone
        List<VenueActivityParticipant> participants = IntStream.range(0, quantity)
                .mapToObj(i -> VenueActivityParticipant.builder()
                        .activityId(activityId)
                        .userId(userId)
                        .status(VenueActivityParticipantStatusEnum.ACTIVE.getValue())
                        .batchId(batchId)
                        .phone(phone)
                        .build())
                .collect(Collectors.toList());

        boolean batchSaveResult = this.saveBatch(participants);
        if (!batchSaveResult) {
            log.error("批量插入参与记录失败 - activityId: {}, userId: {}, quantity: {}",
                    activityId, userId, quantity);
            throw new GloboxApplicationException(VenueCode.VENUE_BOOKING_FAIL);
        }

        log.info("批量报名成功 - activityId: {}, userId: {}, quantity: {}, phone: {}, batchId: {}, participantIds: {}",
                activityId, userId, quantity, phone, batchId,
                participants.stream().map(VenueActivityParticipant::getParticipantId).toList());

        return participants;
    }

}
