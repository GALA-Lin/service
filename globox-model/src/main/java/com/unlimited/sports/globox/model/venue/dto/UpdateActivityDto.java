package com.unlimited.sports.globox.model.venue.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 更新活动DTO
 * @since 2025/01/21
 */
@Data
public class UpdateActivityDto {

    /**
     * 活动名称
     */
    @Size(max = 100, message = "活动名称长度不能超过100字符")
    private String activityName;

    /**
     * 活动描述
     */
    @Size(max = 1000, message = "活动描述长度不能超过1000字符")
    private String description;

    /**
     * 活动图片URL列表
     */
    private List<String> imageUrls;

    /**
     * 最大参与人数
     */
    @Min(value = 1, message = "最大参与人数不能小于1")
    private Integer maxParticipants;


    /**
     * 报名截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registrationDeadline;

    /**
     * 联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    /**
     * 最低NTRP等级要求（1.0-7.0）
     */
    @DecimalMin(value = "1.0", message = "NTRP等级不能小于1.0")
    @DecimalMax(value = "7.0", message = "NTRP等级不能大于7.0")
    private Double minNtrpLevel;

}