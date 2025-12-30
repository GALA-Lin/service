package com.unlimited.sports.globox.model.venue.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 场馆搜索请求DTO
 * 用于MySQL搜索场景，支持距离筛选和场地数量排序
 */
@Data
public class GetVenueListDto {

    /**
     * 关键词搜索（场馆名称、地址、区域）
     */
    private String keyword;

    /**
     * 排序方式：distance（距离）、price（价格）、courtCount（场地数量）
     */
    private String sortBy;

    /**
     * 最低价格过滤
     */
    @DecimalMin(value = "0", message = "最低价格不能小于0")
    private BigDecimal minPrice;

    /**
     * 最高价格过滤
     */
    @DecimalMin(value = "0", message = "最高价格不能小于0")
    private BigDecimal maxPrice;

    /**
     * 场地类型过滤（多选）
     * 1=室内, 2=室外, 3=风雨场, 4=半封闭
     */
    private List<Integer> courtTypes;

    /**
     * 地面类型过滤（多选）
     * 1=硬地, 2=红土, 3=草地, 4=其他
     */
    private List<Integer> groundTypes;

    /**
     * 设施过滤（多选）
     * 传入设施名称列表
     */
    private List<String> facilities;

    /**
     * 场地片数筛选
     * 1=4片以内, 2=4-10片, 3=10片以上
     */
    private Integer courtCountFilter;

    /**
     * 用户位置 - 纬度
     */
    @NotNull(message = "用户位置纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度范围必须在-90到90之间")
    @DecimalMax(value = "90.0", message = "纬度范围必须在-90到90之间")
    private Double latitude;

    /**
     * 用户位置 - 经度
     */
    @NotNull(message = "用户位置经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度范围必须在-180到180之间")
    @DecimalMax(value = "180.0", message = "经度范围必须在-180到180之间")
    private Double longitude;

    /**
     * 距离筛选（公里）
     * 例如：2 表示只显示2公里内的场馆
     * 如果不传或为null，则不限制距离
     */
    @DecimalMin(value = "0.1", message = "距离筛选至少为0.1公里")
    @DecimalMax(value = "100", message = "距离筛选最多为100公里")
    private Double maxDistance;

    /**
     * 预订日期（用于时间段可用性过滤）
     */
    private LocalDate bookingDate;

    /**
     * 开始时间（用于时间段可用性过滤）
     */
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    /**
     * 结束时间（用于时间段可用性过滤）
     */
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于等于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;
}
