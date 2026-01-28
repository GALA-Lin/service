package com.unlimited.sports.globox.model.venue.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建活动请求DTO
 */
@Data
public class CreateActivityDto {

    /**
     * 槽位模板ID列表（必须是连续的时段）
     */
    @NotEmpty(message = "槽位模板ID列表不能为空")
    private List<Long> slotTemplateIds;

    /**
     * 活动类型ID
     */
    @NotNull(message = "活动类型ID不能为空")
    private Long activityTypeId;

    /**
     * 活动名称
     */
    @NotBlank(message = "活动名称不能为空")
    @Size(max = 100, message = "活动名称不能超过100个字符")
    private String activityName;

    /**
     * 活动图片URL列表
     */
    @Size(max = 9, message = "活动图片最多9张")
    private List<String> imageUrls;

    /**
     * 活动日期（不能是过去的日期）
     */
    @NotNull(message = "活动日期不能为空")
    @FutureOrPresent(message = "活动日期不能是过去的日期")
    private LocalDate activityDate;

    /**
     * 联系电话
     */
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String contactPhone;

    /**
     * 最大参与人数（可选）
     */
    @Min(value = 1, message = "最大参与人数至少为1")
    private Integer maxParticipants;

    /**
     * 单人价格（可选）
     */
    @DecimalMin(value = "0.00", message = "单人价格不能为负数")
    private BigDecimal unitPrice;

    /**
     * 活动描述（可选）
     */
    @Size(max = 500, message = "活动描述不能超过500个字符")
    private String description;

    /**
     * 报名截止时间（可选）
     */
    private LocalDateTime registrationDeadline;

    /**
     * 参与用户的最低NTRP水平要求（可选）
     * 范围：1.0 - 7.0
     */
    @DecimalMin(value = "1.0", message = "最低NTRP水平不能低于1.0")
    @DecimalMax(value = "7.0", message = "最低NTRP水平不能高于7.0")
    private Double minNtrpLevel;

    /**
     * 活动配置（JSON格式，可选）
     */
    private String activityConfig;
}
