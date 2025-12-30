package com.unlimited.sports.globox.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.booking.VenuePriceOverride;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 场馆价格覆盖 Mapper
 */
@Mapper
public interface VenuePriceOverrideMapper extends BaseMapper<VenuePriceOverride> {

    /**
     * 查询指定场馆在指定日期和时间段的价格覆盖
     * 用于确定某个时间段的实际价格
     *
     * @param venueId 场馆ID
     * @param courtId 场地ID（可选）
     * @param date 日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 价格覆盖列表（按优先级排序：场地级 > 场馆级）
     */
    List<VenuePriceOverride> selectByVenueAndTimeRange(
            @Param("venueId") Long venueId,
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    /**
     * 查询指定场馆在指定日期的所有价格覆盖
     *
     * @param venueId 场馆ID
     * @param date 日期
     * @return 价格覆盖列表
     */
    List<VenuePriceOverride> selectByVenueIdAndDate(
            @Param("venueId") Long venueId,
            @Param("date") LocalDate date
    );

    /**
     * 查询指定场地在指定日期的所有价格覆盖
     *
     * @param courtId 场地ID
     * @param date 日期
     * @return 价格覆盖列表
     */
    List<VenuePriceOverride> selectByCourtIdAndDate(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date
    );

    /**
     * 批量查询指定场馆列表在指定日期的价格覆盖
     *
     * @param venueIds 场馆ID列表
     * @param date 日期
     * @return 价格覆盖列表
     */
    List<VenuePriceOverride> selectByVenueIdsAndDate(
            @Param("venueIds") List<Long> venueIds,
            @Param("date") LocalDate date
    );
}
