package com.unlimited.sports.globox.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 场馆槽位记录 Mapper
 */
@Mapper
public interface VenueBookingSlotRecordMapper extends BaseMapper<VenueBookingSlotRecord> {

    /**
     * 查询指定日期指定槽位模板的记录
     *
     * @param slotTemplateId 槽位模板ID
     * @param bookingDate 预订日期
     * @return 槽位记录
     */
    VenueBookingSlotRecord selectByTemplateIdAndDate(
            @Param("slotTemplateId") Long slotTemplateId,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 查询指定日期指定场地的所有槽位记录
     *
     * @param courtId 场地ID
     * @param bookingDate 预订日期
     * @return 槽位记录列表
     */
    List<VenueBookingSlotRecord> selectByCourtIdAndDate(
            @Param("courtId") Long courtId,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 查询指定日期指定槽位模板列表的记录
     *
     * @param slotTemplateIds 槽位模板ID列表
     * @param bookingDate 预订日期
     * @return 槽位记录列表
     */
    List<VenueBookingSlotRecord> selectByTemplateIdsAndDate(
            @Param("slotTemplateIds") List<Long> slotTemplateIds,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 批量查询指定场地列表在指定日期的所有槽位记录
     *
     * @param courtIds 场地ID列表
     * @param bookingDate 预订日期
     * @return 槽位记录列表
     */
    List<VenueBookingSlotRecord> selectByCourtIdsAndDate(
            @Param("courtIds") List<Long> courtIds,
            @Param("bookingDate") LocalDate bookingDate
    );

    /**
     * 查询在指定时间段内没有可用场地的场馆ID列表
     * 用于时段预订过滤
     *
     * @param venueIds 要检查的场馆ID列表（可选，为空则检查所有）
     * @param bookingDate 预订日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 没有可用场地的场馆ID列表
     */
    List<Long> selectUnavailableVenueIds(
            @Param("venueIds") List<Long> venueIds,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );



    /**
     * 批量更新槽位记录
     *
     * @param records 槽位记录列表
     * @return 更新的行数
     */
    int updateBatchById(@Param("records") List<VenueBookingSlotRecord> records);
}
