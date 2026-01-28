package com.unlimited.sports.globox.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一搜索结果项 - 泛型包装，支持任意类型的VO
 *
 * @param <T> 具体的数据类型（VenueItemVo, CoachItemVo 等）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultItem<T> {

    /**
     * 数据类型: VENUE | COACH | NOTE | RALLY | USER
     */
    private String dataType;

    /**
     * 具体的VO对象 - 包含所有字段
     */
    private T data;
}
