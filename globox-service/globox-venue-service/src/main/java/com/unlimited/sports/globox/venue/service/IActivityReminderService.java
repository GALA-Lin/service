package com.unlimited.sports.globox.venue.service;

/**
 * 活动提醒服务接口
 */
public interface IActivityReminderService {

    /**
     * 发送活动即将开始提醒延迟消息
     * 根据活动ID和参与者ID查询完整数据，发送延迟消息
     *
     * @param userId 用户ID
     * @param activityId 活动ID
     */
    void sendActivityReminderMessage(Long userId, Long activityId);
}
