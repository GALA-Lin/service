package com.unlimited.sports.globox.venue.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;

import java.util.List;

/**
 * 场馆设施关联Service接口
 */
public interface IVenueFacilityRelationService extends IService<VenueFacilityRelation> {

    /**
     * 批量创建场馆设施关联
     *
     * @param venueId 场馆ID
     * @param facilityIds 设施ID列表
     * @return 成功创建的数量
     */
    int batchCreateFacilities(Long venueId, List<Integer> facilityIds);

}
