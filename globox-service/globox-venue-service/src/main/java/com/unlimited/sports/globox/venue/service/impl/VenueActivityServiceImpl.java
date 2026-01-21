package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivitySlotLock;
import com.unlimited.sports.globox.model.venue.enums.VenueActivityStatusEnum;
import com.unlimited.sports.globox.venue.mapper.VenueActivityMapper;
import com.unlimited.sports.globox.venue.mapper.VenueActivitySlotLockMapper;
import com.unlimited.sports.globox.venue.service.IVenueActivityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 场馆活动 Service 实现
 */
@Slf4j
@Service
public class VenueActivityServiceImpl extends ServiceImpl<VenueActivityMapper, VenueActivity> implements IVenueActivityService {

    @Autowired
    private VenueActivityMapper venueActivityMapper;

    @Autowired
    private VenueActivitySlotLockMapper venueActivitySlotLockMapper;



    /**
     * 批量查询多个活动占用的所有槽位
     * 传入活动ID列表，返回这些活动占用的槽位映射（槽位模板ID -> 活动ID）
     */
    @Override
    public Map<Long, Long> getActivityLockedSlotsByIds(List<Long> activityIds, LocalDate bookingDate) {
        if (activityIds == null || activityIds.isEmpty() || bookingDate == null) {
            return Map.of();
        }

        List<VenueActivitySlotLock> locks = venueActivitySlotLockMapper.selectList(
            new LambdaQueryWrapper<VenueActivitySlotLock>()
                .in(VenueActivitySlotLock::getActivityId, activityIds)
                .eq(VenueActivitySlotLock::getBookingDate, bookingDate)
        );

        return locks.stream()
            .collect(Collectors.toMap(
                VenueActivitySlotLock::getSlotTemplateId,
                VenueActivitySlotLock::getActivityId
            ));
    }

    /**
     * 查询指定场馆指定日期的所有活动
     * 用于批量获取活动信息
     */
    @Override
    public List<VenueActivity> getActivitiesByVenueAndDate(Long venueId, LocalDate activityDate) {
        if (venueId == null || activityDate == null) {
            return new ArrayList<>();
        }
        List<VenueActivity> venueActivities = venueActivityMapper.selectList(new LambdaQueryWrapper<VenueActivity>()
                .eq(VenueActivity::getVenueId, venueId)
                .eq(VenueActivity::getActivityDate, activityDate));
//                .eq(VenueActivity::getStatus, VenueActivityStatusEnum.NORMAL.getValue()));

        return venueActivities == null ? List.of() : venueActivities;
    }
}
