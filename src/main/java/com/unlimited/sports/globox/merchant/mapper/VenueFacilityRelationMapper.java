package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场馆设施关联Mapper
 */
@Mapper
public interface VenueFacilityRelationMapper extends BaseMapper<VenueFacilityRelation> {

    /**
     * 根据设施名称列表查询包含这些设施的场馆ID列表
     * 只返回包含所有指定设施的场馆（AND关系）
     *
     * @param facilityNames 设施名称列表
     * @param facilityCount 设施数量（用于确保场馆包含所有指定设施）
     * @return 场馆ID列表
     */
    List<Long> selectVenueIdsByFacilities(@Param("facilityNames") List<String> facilityNames,
                                          @Param("facilityCount") int facilityCount);

    /**
     * 根据场馆ID列表查询设施名称
     * 用于批量查询多个场馆的设施
     *
     * @param venueIds 场馆ID列表
     * @return 设施名称列表（去重）
     */
    List<String> selectFacilityNamesByVenueIds(@Param("venueIds") List<Long> venueIds);


}
