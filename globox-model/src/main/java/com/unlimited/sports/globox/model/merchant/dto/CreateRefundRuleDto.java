package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

/**
 * @since 2025/12/31 11:01
 * 创建退款规则Dto
 */
@Data
public class CreateRefundRuleDto {

    /**
     * 场馆ID（NULL表示商家默认规则）
     */
    private Long venueId;

    /**
     * 规则名称
     */
    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100字符")
    private String venueRefundRuleName;

    /**
     * 是否为默认规则
     */
    private Boolean isDefault = false;

    /**
     * 规则描述
     */
    @Size(max = 500, message = "规则描述长度不能超过500字符")
    private String venueRefundRuleDesc;

    /**
     * 退款规则明细列表
     */
    @NotEmpty(message = "退款规则明细不能为空")
    @Valid
    private List<RefundRuleDetailDto> details;
}

