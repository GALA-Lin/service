package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * @since 2025-12-18-22:36
 * 创建场地DTO
 */
@Data
public class CourtCreateDto {

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    /**
     * 场地名称
     */
    @NotBlank(message = "场地名称不能为空")
    @Size(max = 100, message = "场地名称长度不能超过100字符")
    private String name;

    /**
     * 场地地面类型
     */
    @NotNull(message = "场地地面类型不能为空")
    private Integer groundType;

    /**
     * 场地类型
     */
    @NotNull(message = "场地类型不能为空")
    private Integer courtType;
}