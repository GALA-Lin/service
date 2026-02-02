package com.unlimited.sports.globox.model.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 统一搜索结果VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnifiedSearchResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 总数
     */
    private Long total;

    /**
     * 总页数
     */
    private Integer totalPages;

    /**
     * 搜索结果列表
     */
    private List<SearchResultItemVo> items;
}
