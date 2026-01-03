package com.unlimited.sports.globox.model.coach.vo;

import com.unlimited.sports.globox.common.result.PaginationResult;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * @since 2026/1/3 10:37
 * 教练列表响应（包含筛选选项）
 */
@Data
@Builder
public class CoachListResponse {

    private Set<String> availableCertifications;

    private Set<String> availableServiceAreas;

    private PaginationResult<CoachItemVo> coaches;

    private PriceRange priceRange;

    /**
     * 价格区间
     */
    @Data
    @Builder
    public static class PriceRange {

        private java.math.BigDecimal minPrice;

        private java.math.BigDecimal maxPrice;

    }
}