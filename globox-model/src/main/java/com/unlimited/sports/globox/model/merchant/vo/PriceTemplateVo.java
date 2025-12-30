package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格模板VO
 */
@Data
@Builder
public class PriceTemplateVo {

    /**
     * 模板ID
     */
    @NonNull
    private Long templateId;

    /**
     * 商家ID
     */
    @NonNull
    private Long merchantId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 是否为默认模板
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    @NonNull
    private Boolean isEnabled;

    /**
     * 价格时段列表
     */
    @NonNull
    private List<PriceTemplatePeriodVo> periods;

    /**
     * 使用该模板的场馆数量
     */
    private Integer venueCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

