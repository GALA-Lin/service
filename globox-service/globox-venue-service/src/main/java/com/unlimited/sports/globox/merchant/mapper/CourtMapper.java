package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-14:07
 * @Description:
 */
@Mapper
public interface CourtMapper extends BaseMapper<Court> {
    /**
     * 根据场馆ID查询场地列表
     * @param venueId 场馆ID
     * @return 场地列表
     */
    @Select("SELECT * FROM courts WHERE venue_id = #{venueId} ORDER BY court_id")
    List<Court> selectByVenueId(@Param("venueId") Long venueId);

    /**
     * 根据场地ID列表查询场地列表
     * @param courtIds 场地ID列表
     * @return 场地列表
     */
    @Select("<script>" +
            "SELECT * FROM courts WHERE court_id IN " +
            "<foreach collection='courtIds' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Court> selectByIds(@Param("courtIds") List<Long> courtIds);

}
