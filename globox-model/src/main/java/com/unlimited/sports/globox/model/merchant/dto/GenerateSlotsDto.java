package com.unlimited.sports.globox.model.merchant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * @since  2025-12-19-09:29
 * 批量生成时段DTO
 */

@Data
public class GenerateSlotsDto {

    /**
     * 场地ID
     */
    @NotNull(message = "场地ID不能为空")
    private Long courtId;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 是否覆盖已存在的时段
     * true: 删除并重新生成
     * false: 跳过已存在的日期
     */
    private Boolean overwrite = false;
}

