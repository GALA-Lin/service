package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * @since 2026/1/8 10:07
 * 教练课程类型DTO
 */
@Data
public class CoachCourseTypeDto {

    /**
     * 教练ID
     */
    private Long coachUserId;

    /**
     * 课程封面URL
     */
    @Size(max = 1024, message = "课程封面URL长度不能超过1024")
    private String courseCover;

    /**
     * 服务名称
     */
    @NotBlank(message = "服务名称不能为空")
    @Size(max = 100, message = "服务名称不能超过100字")
    private String coachCourseTypeName;

    /**
     * 服务类型：1-一对一教学，2-一对一陪练，3-一对二，4-小班(3-6人)
     */
    @NotNull(message = "服务类型不能为空")
    @Min(value = 1, message = "服务类型必须在1-4之间")
    @Max(value = 4, message = "服务类型必须在1-4之间")
    private Integer coachServiceTypeEnum;

    /**
     * 时长（分钟）
     */
    @NotNull(message = "时长不能为空")
    @Min(value = 30, message = "时长最少30分钟")
    @Max(value = 240, message = "时长最多240分钟")
    private Integer coachDuration;

    /**
     * 价格（元）
     */
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @DecimalMax(value = "99999.99", message = "价格不能超过99999.99")
    private BigDecimal coachPrice;

    /**
     * 服务描述
     */
    @Size(max = 500, message = "服务描述不能超过500字")
    private String coachDescription;

    /**
     * 是否启用：0-停用，1-启用
     */
    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态必须为0或1")
    @Max(value = 1, message = "启用状态必须为0或1")
    private Integer coachIsActive = 1;
}
