package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;

/**
 * @since 2025/12/31 13:54
 * 教练列表查询Dto
 */
@Data
public class QueryCoachListDto {

    /**
     * 用户位置 - 纬度（用于距离排序）
     */
    @DecimalMin(value = "-90.0", message = "纬度范围必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度范围必须在-90到90之间")
    private Double latitude;

    /**
     * 用户位置 - 经度（用于距离排序）
     */
    @DecimalMin(value = "-180.0", message = "经度范围必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度范围必须在-180到180之间")
    private Double longitude;

    /**
     * 排序方式：rating-评分排序(默认)，distance-距离排序，price-价格排序
     */
    private String sortBy = "rating";

    /**
     * 最低价格
     */
    @DecimalMin(value = "0", message = "最低价格不能小于0")
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    @DecimalMin(value = "0", message = "最高价格不能小于0")
    private BigDecimal maxPrice;

    /**
     * 常驻区域列表（多选）
     */
    private List<String> serviceAreas;

    /**
     * 资质/证书类型列表（多选）
     * 如：PTR、USPTA、CTA等
     */
    private List<Integer> certificationLevels;

    /**
     * 教龄筛选：1-3年以下，2-3-5年，3-5-8年，4-8年以上
     */
    private Integer teachingYearsFilter;

    /**
     * 性别：1-男，2-女
     */
    private Integer gender;

    /**
     * 课程类型筛选（多选）
     * 1-一对一，2-一对二，3-小班(3-6人)
     */
    private List<Integer> serviceTypes;

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 50, message = "每页大小不能超过50")
    private Integer pageSize = 10;
}
