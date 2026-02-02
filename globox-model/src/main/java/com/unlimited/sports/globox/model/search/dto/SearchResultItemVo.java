package com.unlimited.sports.globox.model.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 搜索结果项VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResultItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据类型: VENUE | NOTE | USER
     */
    private String type;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 具体类型的完整数据（ListItemVo）
     */
    private Object data;

    /**
     * ES相关性分数
     */
    private Float relevanceScore;
}
