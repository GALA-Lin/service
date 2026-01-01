package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * @since 2025/12/31 11:02
 * 查询退款规则Dto
 */
@Data
public class QueryRefundRuleDto {

    /**
     * 场馆ID（可选）
     */
    private Long venueId;

    /**
     * 是否为默认规则
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 规则名称（模糊查询）
     */
    private String venueRefundRuleName;

    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 20;
}
