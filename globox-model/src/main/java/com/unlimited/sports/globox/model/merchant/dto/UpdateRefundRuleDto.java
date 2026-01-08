package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @since 2025/12/31 11:02
 *
 */ // ============= 更新退款规则 DTO =============
@Data
public class UpdateRefundRuleDto {

    /**
     * 规则ID
     */
    @NotNull(message = "规则ID不能为空")
    private Long venueRefundRuleId;

    /**
     * 规则名称
     */
    @Size(max = 100, message = "规则名称长度不能超过100字符")
    private String venueRefundRuleName;

    /**
     * 是否为默认规则
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 规则描述
     */
    @Size(max = 500, message = "规则描述长度不能超过500字符")
    private String venueRefundRuleDesc;

    /**
     * 退款规则明细列表（如果提供，将覆盖原有明细）
     */
    @Valid
    private List<RefundRuleDetailDto> details;
}
