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
    private List<DictItem> courtTypes;

    /**
     * 地面类型字典
     */
    private List<DictItem> groundTypes;

    /**
     * 场地片数筛选字典
     */
    private List<DictItem> courtCountFilters;

    /**
     * 距离筛选字典
     */
    private List<DictItem> distances;

    /**
     * 设施类型字典
     */
    private List<DictItem> facilities;

    /**
     * 字典项结构
     * todo 后续后台管理实现后移除,使用统一的字典结构item
     */

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DictItem {
        /**
         * 值（用于前端提交参数）
         */
        private Integer value;

        /**
         * 描述（用于前端显示）
         */
        private String description;
    }
}
