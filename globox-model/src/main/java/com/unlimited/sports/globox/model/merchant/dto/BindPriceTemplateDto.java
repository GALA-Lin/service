package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 绑定价格模板到场馆DTO
 */
@Data
public class BindPriceTemplateDto {

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 是否刷新已生成的时段价格
     * true: 将重新生成未来所有未支付的时段
     * false: 只对新生成的时段生效
     */
    private Boolean refreshExistingSlots = false;
}
