package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.merchant.mapper.VenueBusinessHoursMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;
import com.unlimited.sports.globox.model.merchant.enums.BusinessHourRuleTypeEnum;
import com.unlimited.sports.globox.venue.service.IVenueBusinessHoursService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
public class VenueBusinessHoursService implements IVenueBusinessHoursService {


    @Autowired
    private VenueBusinessHoursMapper venueBusinessHoursMapper;
    /**
     * 获取场馆指定日期的营业时间配置
     * 优先级：特定日期规则 > 常规规则
     */
    @Override
    public VenueBusinessHours getBusinessHoursByDate(Long venueId, LocalDate date) {
        // 首先查找特定日期的规则（如果是特殊日期或关闭日期）
        VenueBusinessHours specialHours = venueBusinessHoursMapper.selectOne(
                new LambdaQueryWrapper<VenueBusinessHours>()
                        .eq(VenueBusinessHours::getVenueId, venueId)
                        .eq(VenueBusinessHours::getEffectiveDate, date)
                        .orderByDesc(VenueBusinessHours::getPriority)
                        .last("LIMIT 1")
        );

        if (specialHours != null) {
            return specialHours;
        }

        // 查找常规营业时间规则
        VenueBusinessHours regularHours = venueBusinessHoursMapper.selectOne(
                new LambdaQueryWrapper<VenueBusinessHours>()
                        .eq(VenueBusinessHours::getVenueId, venueId)
                        .eq(VenueBusinessHours::getRuleType, BusinessHourRuleTypeEnum.REGULAR.getCode())
                        .orderByDesc(VenueBusinessHours::getPriority)
                        .last("LIMIT 1")
        );

        return regularHours;
    }


    /**
     * 获取场馆常规营业时间
     * @param venueId 场馆id
     */
    @Override
    public VenueBusinessHours getRegularBusinessHours(Long venueId) {
        return venueBusinessHoursMapper.selectOne(
                new LambdaQueryWrapper<VenueBusinessHours>()
                        .eq(VenueBusinessHours::getVenueId, venueId)
                        .eq(VenueBusinessHours::getRuleType, BusinessHourRuleTypeEnum.REGULAR.getCode())
                        .orderByDesc(VenueBusinessHours::getCreatedAt)
                        .last("LIMIT 1")
        );
    }

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
    @Override
    public List<Long> getUnavailableVenueIds(LocalDate bookingDate, LocalTime startTime, LocalTime endTime) {
        log.info("查询不可预订场馆: targetDate={}, startTime={}, endTime={}", bookingDate, startTime, endTime);

        List<Long> unavailableIds = venueBusinessHoursMapper.selectUnavailableVenueIdsByBusinessHours(
                bookingDate, startTime, endTime
        );

        log.info("基于营业时间不可预订场馆数量: {}", unavailableIds != null ? unavailableIds.size() : 0);
        return unavailableIds;
    }


}
