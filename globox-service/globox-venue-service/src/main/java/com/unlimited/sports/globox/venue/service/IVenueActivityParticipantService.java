package com.unlimited.sports.globox.venue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;

/**
 * 活动参与者 Service 接口
 */
public interface IVenueActivityParticipantService extends IService<VenueActivityParticipant> {

    /**
     * 用户报名活动
     * 原子操作：检查人数 + 增加参与人数 + 插入参与记录
     * 防止超卖问题
     *
     * @param activityId 活动ID
     * @param userId 用户ID
     * @return 报名成功返回参与记录，失败抛出异常
     */
    VenueActivityParticipant registerUserToActivity(Long activityId, Long userId);

}
