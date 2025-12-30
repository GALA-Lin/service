package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**

 * @since 2025-12-18-22:39
 * 场地更新DTO
 */
@Data
public class CourtUpdateDto {

    /**
     * 场地ID
     */
    @NotNull(message = "场地ID不能为空")
    private Long courtId;

    /**
     * 场地名称
     */
    @Size(max = 100, message = "场地名称长度不能超过100字符")
    private String name;

    /**
     * 场地地面类型
     */
    private Integer groundType;

    /**
     * 场地类型
     */
    private Integer courtType;

    /**
     * 状态
     */
    private Integer status;
}
