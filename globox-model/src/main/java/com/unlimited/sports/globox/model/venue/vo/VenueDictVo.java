package com.unlimited.sports.globox.model.venue.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场馆搜索字典数据VO
 * 包含前端搜索过滤时所需的所有字典数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VenueDictVo {

    /**
     * 场地类型字典
     */
    private List<VenueDictItem> courtTypes;

    /**
     * 地面类型字典
     */
    private List<VenueDictItem> groundTypes;

    /**
     * 场地片数筛选字典
     */
    private List<VenueDictItem> courtCountFilters;

    /**
     * 距离筛选字典
     */
    private List<VenueDictItem> distances;

    /**
     * 设施类型字典
     */
    private List<VenueDictItem> facilities;
}
