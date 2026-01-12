package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalTime;
import java.util.List;

/**
 * @since 2025/12/27 13:53
 * 时间槽位模板Mapper
 */
@Mapper
public interface MerchantVenueBookingSlotTemplateMapper extends BaseMapper<VenueBookingSlotTemplate> {
    /**
     * 根据场地ID查询模板列表，按时间排序
     * @param courtId 场地ID
     * @return 模板列表
     */
    List<VenueBookingSlotTemplate> selectByCourtIdOrderByTime(@Param("courtId") Long courtId);

    /**
     * 批量插入模板
     *
     * @param templates 模板列表
     * @return 影响行数
     */
    int batchInsert(@Param("templates") List<VenueBookingSlotTemplate> templates);


    /**
     * 根据场地ID删除模板
     * @param courtId 场地ID
     * @return 影响行数
     */
    int deleteByCourtId(@Param("courtId") Long courtId);

    /**
     * 批量查询指定场地列表在指定时间范围内的槽位模板
     *
     * @param courtIds 场地ID列表
     * @param openTime 开始时间（营业时间）
     * @param closeTime 结束时间（营业时间）
     * @return 槽位模板列表
     */
    List<VenueBookingSlotTemplate> MerchantSelectByCourtIdsAndTimeRange(
            @Param("courtIds") List<Long> courtIds,
            @Param("openTime") LocalTime openTime,
            @Param("closeTime") LocalTime closeTime
    );}