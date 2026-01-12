package com.unlimited.sports.globox.venue.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 创建活动DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateActivityDto {

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    /**
     * 场地ID
     */
    @NotNull(message = "场地ID不能为空")
    private Long courtId;

    /**
     * 活动类型ID
     */
    @NotNull(message = "活动类型ID不能为空")
    private Long activityTypeId;

    /**
     * 活动类型描述
     */
    @NotBlank(message = "活动类型描述不能为空")
    private String activityTypeDesc;

    /**
     * 活动名称
     */
    @NotBlank(message = "活动名称不能为空")
    private String activityName;

    /**
     * 活动日期
     */
    @NotNull(message = "活动日期不能为空")
    @Future(message = "活动日期必须是未来日期")
    private LocalDate activityDate;

    /**
     * 开始时间（必须是整点或半点，如 08:00、08:30）
     */
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    /**
     * 结束时间（必须是整点或半点，如 10:00、10:30）
     */
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    /**
     * 最大参与人数
     */
    @Min(value = 1, message = "最大参与人数至少为1")
    private Integer maxParticipants;

    /**
     * 单人价格
     */
    @DecimalMin(value = "0.00", message = "价格不能为负数")
    private BigDecimal unitPrice;

    /**
     * 组织者ID
     */
    @NotNull(message = "组织者ID不能为空")
    private Long organizerId;

    /**
     * 组织者类型：1=MERCHANT(商家)，2=ADMIN(管理员)
     */
    @NotNull(message = "组织者类型不能为空")
    @Min(value = 1, message = "组织者类型必须为1或2")
    @Max(value = 2, message = "组织者类型必须为1或2")
    private Integer organizerType;

    /**
     * 最低NTRP水平要求（可选）
     */
    private Double minNtrpLevel;

    /**
     * 活动描述（可选）
     */
    private String description;
}
