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



}
