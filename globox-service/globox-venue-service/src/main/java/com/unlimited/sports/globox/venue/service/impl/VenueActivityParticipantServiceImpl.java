package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivityParticipantMapper;
import com.unlimited.sports.globox.venue.service.IVenueActivityParticipantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 活动参与者 Service 实现
 */
@Service
@Slf4j
public class VenueActivityParticipantServiceImpl extends ServiceImpl<VenueActivityParticipantMapper, VenueActivityParticipant>
        implements IVenueActivityParticipantService {

    @Autowired
    private VenueActivityMapper activityMapper;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public VenueActivityParticipant registerUserToActivity(Long activityId, Long userId) {
        if (activityId == null || userId == null) {
            throw new GloboxApplicationException("活动ID和用户ID不能为空");
        }

        // 查询活动详情
        VenueActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new GloboxApplicationException("活动不存在");
        }

        // 检查是否已经报名（快速失败路径）
        VenueActivityParticipant existing = this.getOne(
                new LambdaQueryWrapper<VenueActivityParticipant>()
                        .eq(VenueActivityParticipant::getActivityId, activityId)
                        .eq(VenueActivityParticipant::getUserId, userId)
        );
        if (existing != null) {
            throw new GloboxApplicationException("您已经报名过该活动，无法重复报名");
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
            throw new GloboxApplicationException("活动名额已满，无法报名");
        }

        log.info("活动人数已增加 - activityId: {}, userId: {}", activityId, userId);

        VenueActivityParticipant participant = VenueActivityParticipant.builder()
                .activityId(activityId)
                .userId(userId)
                .build();

        this.save(participant);
        log.info("用户报名成功 - activityId: {}, userId: {}, participantId: {}",
                activityId, userId, participant.getParticipantId());
        return participant;
    }

}
