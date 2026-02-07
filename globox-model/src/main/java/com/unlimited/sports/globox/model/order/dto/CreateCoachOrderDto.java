package com.unlimited.sports.globox.model.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 创建订单dto
 */
@Data
@Schema(name = "CreateCoachOrderDto", description = "创建场地订单请求参数")
public class CreateCoachOrderDto {

    @NotNull(message = "教练 ID 不能为空")
    @Schema(description = "教练 ID",
            example = "1120",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long coachId;

    @NotNull(message = "服务类型 ID 不能为空")
    @Schema(description = "服务类型 ID",
            example = "1120",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long serviceTypeId;

    @Schema(description = "联系人姓名",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String contactName;

    @Schema(description = "联系电话",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @NotNull(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String userPhone;

    @Schema(description = "学员人数",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer studentCount;

    @Schema(description = "特殊需求说明",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String specialRequirements;

    /**
     * 预订日期
     */
    @FutureOrPresent(message = "预定日期不能早于今天")
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
