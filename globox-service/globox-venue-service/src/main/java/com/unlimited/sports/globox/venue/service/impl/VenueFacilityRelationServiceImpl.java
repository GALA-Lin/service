package com.unlimited.sports.globox.venue.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.unlimited.sports.globox.merchant.mapper.VenueFacilityRelationMapper;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueFacilityRelation;
import com.unlimited.sports.globox.model.venue.enums.FacilityType;
import com.unlimited.sports.globox.venue.service.IVenueFacilityRelationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 场馆设施关联Service实现
 */
@Slf4j
@Service
public class VenueFacilityRelationServiceImpl extends ServiceImpl<VenueFacilityRelationMapper, VenueFacilityRelation>
        implements IVenueFacilityRelationService {

    @Override
    public int batchCreateFacilities(Long venueId, List<Integer> facilityIds) {
        if (CollectionUtils.isEmpty(facilityIds)) {
            return 0;
        }

        List<VenueFacilityRelation> relations = facilityIds.stream()
                .map(FacilityType::fromValue)
                .filter(facilityType -> facilityType != null)
                .map(facilityType -> {
                    VenueFacilityRelation relation = new VenueFacilityRelation();
                    relation.setVenueId(venueId);
                    relation.setFacilityId(facilityType.getValue());
                    relation.setFacilityName(facilityType.getDescription());
                    return relation;
                })
                .toList();

        if (relations.isEmpty()) {
            return 0;
        }

        // 批量插入
        boolean success = this.saveBatch(relations);
        log.info("场馆{}批量创建设施关系: 成功插入{}条", venueId, success ? relations.size() : 0);

        return success ? relations.size() : 0;
    }
}
