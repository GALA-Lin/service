package com.unlimited.sports.globox.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.booking.VenueBookingSlotTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalTime;
import java.util.List;

/**
 * 场馆槽位模板 Mapper
 */
@Mapper
public interface VenueBookingSlotTemplateMapper extends BaseMapper<VenueBookingSlotTemplate> {

    /**
     * 查询指定场地在指定时间范围内的槽位模板
     *
     * @param courtId 场地ID
     * @param openTime 开始时间（营业时间）
     * @param closeTime 结束时间（营业时间）
     * @return 槽位模板列表
     */
    List<VenueBookingSlotTemplate> selectByCourtIdAndTimeRange(
            @Param("courtId") Long courtId,
            @Param("openTime") LocalTime openTime,
            @Param("closeTime") LocalTime closeTime
    );

    /**
     * 查询指定场地的所有槽位模板
     *
     * @param courtId 场地ID
     * @return 槽位模板列表
     */
    List<VenueBookingSlotTemplate> selectByCourtId(@Param("courtId") Long courtId);

    /**
     * 查询指定场馆下所有场地的槽位模板
     *
     * @param courtIds 场地ID列表
     * @return 槽位模板列表
     */
    List<VenueBookingSlotTemplate> selectByCourtIds(@Param("courtIds") List<Long> courtIds);

    /**
     * 批量查询指定场地列表在指定时间范围内的槽位模板
     *
     * @param courtIds 场地ID列表
     * @param openTime 开始时间（营业时间）
     * @param closeTime 结束时间（营业时间）
     * @return 槽位模板列表
     */
    List<VenueBookingSlotTemplate> selectByCourtIdsAndTimeRange(
            @Param("courtIds") List<Long> courtIds,
            @Param("openTime") LocalTime openTime,
            @Param("closeTime") LocalTime closeTime
    );
}
