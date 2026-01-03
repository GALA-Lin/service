package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 时段模板更新DTO
 */
@Data
public class CoachSlotTemplateUpdateDto {

    /**
     * 教练ID
     */
    @NotNull(message = "教练ID不能为空")
    private Long coachUserId;

    /**
     * 时段模板ID
     */
    private Long coachServiceId;

    /**
     * 价格
     */
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    private BigDecimal price;

    /**
     * 时段模板名称
     */
    private List<String> acceptableAreas;
    /**
     * 场地要求说明
     */
    private String venueRequirementDes;

    @Min(value = 1, message = "至少提前1天开放")
    @Max(value = 30, message = "最多提前30天开放")
    private Integer advanceBookingDays;
}
