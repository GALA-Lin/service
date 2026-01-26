package com.unlimited.sports.globox.venue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivityParticipant;

import java.util.List;

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

    /**
     * 批量报名活动（支持多个名额）
     * 原子操作：检查人数 + 一次性增加参与人数 + 批量插入参与记录
     * 如果名额不足或插入失败，整个事务回滚，一个名额都不占用
     *
     * @param activityId 活动ID
     * @param userId 用户ID
     * @param quantity 报名名额数
     * @param phone 用户手机号
     * @return 报名成功返回插入的参与记录列表，失败抛出异常
     */
    List<VenueActivityParticipant> registerMultipleParticipants(Long activityId, Long userId, Integer quantity, String phone);

}
