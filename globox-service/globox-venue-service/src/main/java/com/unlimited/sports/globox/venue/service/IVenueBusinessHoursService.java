package com.unlimited.sports.globox.venue.service;

import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 场馆营业时间管理
 */
public interface IVenueBusinessHoursService {

    /**
     * 根据指定时间获取当天的营业时间
     * @param venueId 场馆id
     * @param date 日期
     * @return 营业时间配置
     */
     VenueBusinessHours getBusinessHoursByDate(Long venueId, LocalDate date);


    /**
     * 获取场馆常规营业时间
     * @param venueId 场馆id
     */
     VenueBusinessHours getRegularBusinessHours(Long venueId);

    /**
     * 查询指定时段内不可预订的场馆ID列表
     * 基于营业时间规则优先级：关闭 > 特殊 > 常规
     * 如果场馆没有任何营业时间配置，则默认不营业
     *
     * @param bookingDate 预订日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 不可预订的场馆ID列表
     */
    List<Long> getUnavailableVenueIds(LocalDate bookingDate, LocalTime startTime, LocalTime endTime);

}
