package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 更新价格模板DTO
 */
@Data
public class UpdatePriceTemplateDto {

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * 模板名称
     */
    @Size(max = 100, message = "模板名称长度不能超过100字符")
    private String templateName;

    /**
     * 是否为默认模板
     */
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 价格时段列表（如果提供，将覆盖原有时段）
     */
    private List<PriceTemplatePeriodDto> periods;
}
