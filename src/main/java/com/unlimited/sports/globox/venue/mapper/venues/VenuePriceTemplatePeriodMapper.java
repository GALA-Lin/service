package com.unlimited.sports.globox.venue.mapper.venues;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenuePriceTemplatePeriod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalTime;
import java.util.List;

/**
 * 价格时段Mapper
 */
@Mapper
public interface VenuePriceTemplatePeriodMapper extends BaseMapper<VenuePriceTemplatePeriod> {

    /**
     * 根据模板ID查询所有启用的价格时段
     * @param templateId 模板ID
     * @return 价格时段列表
     */
    @Select("SELECT * FROM venue_price_template_period " +
            "WHERE template_id = #{templateId} " +
            "AND is_enabled = 1 " +
            "ORDER BY start_time")
    List<VenuePriceTemplatePeriod> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 查询指定时间点所属的价格时段
     * @param templateId 模板ID
     * @param time 时间点
     * @return 价格时段
     */
    @Select("SELECT * FROM venue_price_template_period " +
            "WHERE template_id = #{templateId} " +
            "AND is_enabled = 1 " +
            "AND start_time <= #{time} " +
            "AND end_time > #{time} " +
            "LIMIT 1")
    VenuePriceTemplatePeriod selectByTemplateIdAndTime(
            @Param("templateId") Long templateId,
            @Param("time") LocalTime time
    );
}
