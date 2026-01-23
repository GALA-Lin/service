package com.unlimited.sports.globox.venue.service;

import java.util.List;

/**
 * 订场提醒服务接口
 */
public interface IVenueBookingReminderService {

    /**
     * 发送订场即将开始提醒延迟消息
     * 根据recordIds查询完整数据，自动按场地和连续时间段分组并发送延迟消息
     *
     * @param userId 用户ID
     * @param recordIds 槽位记录ID列表
     * @param orderNo 订单号
     */
    void sendBookingReminderMessages(Long userId, List<Long> recordIds, Long orderNo);
}
