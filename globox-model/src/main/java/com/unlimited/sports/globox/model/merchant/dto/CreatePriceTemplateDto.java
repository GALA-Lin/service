package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 创建价格模板DTO
 */
@Data
public class CreatePriceTemplateDto {

    /**
     * 模板名称
     */
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称长度不能超过100字符")
    private String templateName;

    /**
     * 是否为默认模板
     */
    private Boolean isDefault = false;

    /**
     * 价格时段列表
     */
    @NotEmpty(message = "价格时段列表不能为空")
    private List<PriceTemplatePeriodDto> periods;
}

