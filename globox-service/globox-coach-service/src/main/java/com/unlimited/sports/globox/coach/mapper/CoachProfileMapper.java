package com.unlimited.sports.globox.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.coach.entity.CoachProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @since 2025/12/31 16:46
 * 教练档案 Mapper
 */
@Mapper
public interface CoachProfileMapper extends BaseMapper<CoachProfile> {

    /**
     * 搜索教练（支持多条件筛选、距离计算和排序）
     *
     * @param keyword 关键词（教练名称、服务区域）
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param serviceAreas 服务区域列表
     * @param certifications 资质列表
     * @param minYears 最小教龄
     * @param maxYears 最大教龄
     * @param gender 性别
     * @param serviceTypes 服务类型列表
     * @param latitude 用户纬度
     * @param longitude 用户经度
     * @param maxDistance 最大距离（公里）
     * @param sortBy 排序方式：rating-评分，distance-距离
     * @param offset 分页偏移量
     * @param pageSize 每页大小
     * @return 教练列表（包含距离信息）
     */
    List<Map<String, Object>> searchCoaches(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("serviceAreas") List<String> serviceAreas,
            @Param("certifications") List<String> certifications,
            @Param("minYears") Integer minYears,
            @Param("maxYears") Integer maxYears,
            @Param("gender") Integer gender,
            @Param("serviceTypes") List<Integer> serviceTypes,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("maxDistance") BigDecimal maxDistance,
            @Param("sortBy") String sortBy,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );

    /**
     * 统计搜索结果总数
     */
    long countSearchCoaches(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("serviceAreas") List<String> serviceAreas,
            @Param("certifications") List<String> certifications,
            @Param("minYears") Integer minYears,
            @Param("maxYears") Integer maxYears,
            @Param("gender") Integer gender,
            @Param("serviceTypes") List<Integer> serviceTypes,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude,
            @Param("maxDistance") BigDecimal maxDistance
    );
}