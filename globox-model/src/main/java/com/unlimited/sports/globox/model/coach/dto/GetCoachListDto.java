package com.unlimited.sports.globox.model.coach.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * @since 2025/12/31 13:54
 * 教练列表查询Dto
 */
@Data
public class GetCoachListDto {

    /**
     * 页码
     */
    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小必须大于0")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;

    /**
     * 排序方式：rating-按评分，distance-按距离
     * 默认按评分排序
     */
    private String sortBy = "rating";

    /**
     * 用户当前纬度（用于距离计算和排序）
     */
    private BigDecimal latitude;

    /**
     * 用户当前经度（用于距离计算和排序）
     */
    private BigDecimal longitude;

    /**
     * 最大距离（公里），用于筛选
     */
    private BigDecimal maxDistance;

    /**
     * 关键词搜索（教练名称、服务区域）
     */
    private String keyword;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 常驻区域列表（支持多选）
     */
    private List<String> serviceAreas;

    /**
     * 资质类型列表（支持多选）
     * 例如：["PTR初级", "PTR中级", "USPTA"]
     */
    private List<String> certifications;

    /**
     * 教龄筛选：1-小于3年，2-3-5年，3-5-8年，4-8年以上
     */
    private Integer teachingYearsFilter;

    /**
     * 性别筛选：0-女，1-男
     */
    private Integer gender;

    /**
     * 课程类型筛选：1-一对一，2-一对二，3-小班(3-6人)
     */
    private List<Integer> serviceTypes;
}