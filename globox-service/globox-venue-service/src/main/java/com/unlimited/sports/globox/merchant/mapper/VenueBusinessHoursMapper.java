package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueBusinessHours;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-14:37
 * @Description:
 */
@Mapper
public interface VenueBusinessHoursMapper extends BaseMapper<VenueBusinessHours> {
    /**
     * 查询场馆的所有营业时间规则
     * @param venueId 场馆ID
     * @return 营业时间规则列表
     */
    @Select("SELECT * FROM venue_business_hours WHERE venue_id = #{venueId}")
    List<VenueBusinessHours> selectByVenueId(@Param("venueId") Long venueId);

    /**
     * 查询场馆在指定日期的营业时间规则
     * @param venueId 场馆ID
     * @param date 日期
     * @param dayOfWeek 星期
     * @return 营业时间规则列表
     */
    @Select("SELECT * FROM venue_business_hours " +
            "WHERE venue_id = #{venueId} " +
            "AND (" +
            "    (rule_type = 3 AND effective_date = #{date}) " +
            "    OR (rule_type = 2 AND effective_date = #{date}) " +
            "    OR (rule_type = 1 AND day_of_week = #{dayOfWeek}) " +
            ") " +
            "ORDER BY priority DESC, rule_type DESC")
    List<VenueBusinessHours> selectByVenueIdAndDate(
            @Param("venueId") Long venueId,
            @Param("date") LocalDate date,
            @Param("dayOfWeek") Integer dayOfWeek
    );

    /**
     * 查询指定时段内不可预订的场馆ID列表
     * 基于营业时间规则优先级：关闭 > 特殊 > 常规
     * 如果场馆没有任何营业时间配置，则默认不营业
     *
     * @param targetDate 预订日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 不可预订的场馆ID列表
     */
    List<Long> selectUnavailableVenueIdsByBusinessHours(
            @Param("targetDate") LocalDate targetDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
