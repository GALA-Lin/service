package com.unlimited.sports.globox.model.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

/**
 * 批量初始化场地时段模板DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTemplateInitDto {

    /**
     * 场地ID列表（必填，最多50个）
     */
    @NotEmpty(message = "场地ID列表不能为空")
    @Size(max = 50, message = "单次最多支持50个场地")
    private List<Long> courtIds;

    /**
     * 开放时间（必填）
     */
    @NotNull(message = "开放时间不能为空")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime openTime;

    /**
     * 关闭时间（必填）
     */
    @NotNull(message = "关闭时间不能为空")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    /**
     * 是否覆盖已有模板（默认覆盖）
     * true - 删除旧模板重新创建
     * false - 已有模板的场地跳过
     */
    private Boolean overwrite = true;
}