package com.unlimited.sports.globox.model.merchant.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

/**
 * 更新场馆DTO
 * @since 2026-01-15
 */
@Data
public class VenueUpdateDto {

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    /**
     * 场馆官方名称
     */
    @Size(max = 100, message = "场馆名称长度不能超过100字符")
    private String name;

    /**
     * 场馆详细地址
     */
    @Size(max = 255, message = "地址长度不能超过255字符")
    private String address;

    /**
     * 所属区域或行政区
     */
    @Size(max = 50, message = "区域名称长度不能超过50字符")
    private String region;

    /**
     * 纬度
     */
    @DecimalMin(value = "-90.0", message = "纬度必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度必须在-90到90之间")
    private BigDecimal latitude;

    /**
     * 经度
     */
    @DecimalMin(value = "-180.0", message = "经度必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度必须在-180到180之间")
    private BigDecimal longitude;

    /**
     * 场馆联系电话
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号码")
    private String phone;

    /**
     * 场馆文字介绍或简介
     */
    @Size(max = 1000, message = "场馆介绍长度不能超过1000字符")
    private String description;

    /**
     * 场馆设施标签列表
     */
    private String facilities;

    /**
     * 提前几天开放订场
     */
    @Min(value = 1, message = "提前开放天数至少为1天")
    @Max(value = 90, message = "提前开放天数最多为90天")
    private Integer maxAdvanceDays = 7;

    /**
     * 几点开放订场
     */
    private LocalTime slotVisibilityTime = LocalTime.parse("00:00:00");

    /**
     * 场馆状态
     */
    private Integer status;

    /**
     * 图片URL列表（可选，用于更新图片）
     */
    private List<String> imageUrls;

    /**
     * 是否清空原有图片（true时会删除原有图片）
     */
    private Boolean clearImages = true;

    /**
     * 营业时间规则列表
     * - 如果提供，将完全覆盖原有营业时间
     * - 如果不提供，则不修改营业时间
     */
    @Valid
    private List<VenueBusinessHoursDto> businessHours;

    /**
     * 是否清空原有营业时间（仅当businessHours为空时生效）
     */
    private Boolean clearBusinessHours = false;
}