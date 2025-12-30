package com.unlimited.sports.globox.model.merchant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * @since 2025-12-19-09:29
 * 生成时段DTO
 */

@Data
public class GenerateDailySlotsDto {

    /**
     * 场地ID
     */
    @NotNull(message = "场地ID不能为空")
    private Long courtId;

    /**
     * 日期
     */
    @NotNull(message = "日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 是否覆盖已存在的时段
     */
    private Boolean overwrite = false;
}
