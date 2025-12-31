package com.unlimited.sports.globox.venue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueExtraChargeTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场馆额外费用模板 Mapper
 */
@Mapper
public interface VenueExtraChargeTemplateMapper extends BaseMapper<VenueExtraChargeTemplate> {

    // 使用 MyBatis Plus 的 LambdaQueryWrapper 进行查询，无需在此定义方法
    // 在service层使用 query().eq(...).list() 等方法
}
