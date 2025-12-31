package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueBookingSlotTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}