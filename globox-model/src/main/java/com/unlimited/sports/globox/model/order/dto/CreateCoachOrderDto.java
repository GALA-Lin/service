package com.unlimited.sports.globox.model.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单dto
 */
@Data
@Schema(name = "CreateVenueOrderDto", description = "创建场地订单请求参数")
public class CreateCoachOrderDto {

    /**
     * 预订日期
     */
    @NotNull(message = "预订日期不能为空")
    @Schema(description = "预订日期（格式：yyyy-MM-dd）",
            example = "2025-12-20",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate bookingDate;

    /**
     * 预订的场地时段 ID 列表
     */
    @NotNull(message = "预定的时间段不能为空")
    @Size(min = 1, message = "预定的时间段不能为空")
    @Schema(description = "预订的时段ID列表，至少选择一个",
            example = "[101,102,103]",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> slotIds;
}
