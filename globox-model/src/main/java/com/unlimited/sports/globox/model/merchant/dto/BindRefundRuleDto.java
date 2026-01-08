package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @since 2025/12/31 11:02
 * 绑定退款规则到场馆Dto
 */
@Data
public class BindRefundRuleDto {

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    /**
     * 规则ID
     */
    @NotNull(message = "规则ID不能为空")
    private Long venueRefundRuleId;
}
