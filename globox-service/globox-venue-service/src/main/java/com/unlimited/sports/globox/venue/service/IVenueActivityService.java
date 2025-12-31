package com.unlimited.sports.globox.venue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 场馆活动 Service 接口
 */
public interface IVenueActivityService extends IService<VenueActivity> {





    /**
     * 批量查询多个槽位是否被活动占用
     * 返回被占用槽位的映射信息
     *
     * @param slotTemplateIds 槽位模板ID列表
     * @param bookingDate 活动日期
     * @return Map<槽位模板ID, 活动ID>
     */
    Map<Long, Long> getActivityLockedSlotsByIds(List<Long> slotTemplateIds, LocalDate bookingDate);

    /**
     * 查询指定场馆指定日期的所有活动（包括所有状态）
     * 用于批量获取活动信息
     *
     * @param venueId 场馆ID
     * @param activityDate 活动日期
     * @return 活动列表
     */
    List<VenueActivity> getActivitiesByVenueAndDate(Long venueId, LocalDate activityDate);
}
