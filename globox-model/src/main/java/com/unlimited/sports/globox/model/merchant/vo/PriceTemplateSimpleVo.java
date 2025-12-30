package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

/**
 * 价格模板简要信息VO（用于列表展示）
 */
@Data
@Builder
public class PriceTemplateSimpleVo {

    /**
     * 模板ID
     */
    @NonNull
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 是否为默认模板
     */
    @NonNull
    private Boolean isDefault;

    /**
     * 是否启用
     */
    @NonNull
    private Boolean isEnabled;

    /**
     * 时段数量
     */
    @NonNull
    private Integer periodCount;

    /**
     * 价格区间描述（如：60-200元）
     */
    @NonNull
    private String priceRange;

    /**
     * 使用该模板的场馆数量
     */
    private Integer venueCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
