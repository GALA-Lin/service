package com.unlimited.sports.globox.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.social.entity.FilterRegion;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FilterRegionMapper extends BaseMapper<FilterRegion> {

    @Select("select * from filter_region")
    List<FilterRegion> selectAll();

}
