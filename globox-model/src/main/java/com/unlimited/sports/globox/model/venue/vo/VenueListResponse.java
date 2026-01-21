package com.unlimited.sports.globox.model.venue.vo;

import com.unlimited.sports.globox.common.result.PaginationResult;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 场馆列表响应（包含筛选选项）
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VenueListResponse {

    /**
     * 场馆分页列表
     */
    @NonNull
    private PaginationResult<VenueItemVo> venues = PaginationResult.build(new ArrayList<>(), 0L, 1, 10);

    /**
     * 场地类型字典
     */
    @NonNull
    private List<VenueDictItem> courtTypes = new ArrayList<>();

    /**
     * 地面类型字典
     */
    @NonNull
    private List<VenueDictItem> groundTypes = new ArrayList<>();

    /**
     * 场地片数筛选字典
     */
    @NonNull
    private List<VenueDictItem> courtCountFilters = new ArrayList<>();

    /**
     * 距离筛选字典
     */
    @NonNull
    private List<VenueDictItem> distances = new ArrayList<>();

    /**
     * 设施类型字典
     */
    @NonNull
    private List<VenueDictItem> facilities = new ArrayList<>();

    /**
     * 价格区间
     */
    private PriceRange priceRange;

    /**
     * 价格区间
     */
    @Data
    @Builder
    public static class PriceRange {
        /**
         * 最小价格
         */
        private BigDecimal minPrice;

        /**
         * 最大价格
         */
        private BigDecimal maxPrice;
    }
}
