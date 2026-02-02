package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 绑定价格模板到场地DTO（批量）
 */
@Data
public class BindPriceTemplateDto {

    /**
     * 场地ID列表（支持批量绑定）
     */
    @NotEmpty(message = "场地ID列表不能为空")
    private List<Long> courtIds;

    /**
     * 模板ID
     */
    @NotNull(message = "模板ID不能为空")
    private Long templateId;


}