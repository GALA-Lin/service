package com.unlimited.sports.globox.model.venue.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * 场馆字典项通用数据结构
 * 用于前端显示字典数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VenueDictItem {

    /**
     * 值（用于前端提交参数）
     */
    @NonNull
    private Integer value;

    /**
     * 描述（用于前端显示）
     */
    @NonNull
    private String description;
}
