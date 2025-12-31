package com.unlimited.sports.globox.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 场馆活动 Mapper
 */
@Mapper
public interface VenueActivityMapper extends BaseMapper<VenueActivity> {



    /**
     * 查询指定场馆指定日期的活动列表
     */
    List<VenueActivity> selectByVenueAndDate(
            @Param("venueId") Long venueId,
            @Param("activityDate") LocalDate activityDate
    );


}
