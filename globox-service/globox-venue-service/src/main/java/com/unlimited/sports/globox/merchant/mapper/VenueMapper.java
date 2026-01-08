package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @Author: Linsen Hu
 * @Date: 2025-12-18-14:05
 * @Description:
 */
@Mapper
public interface VenueMapper extends BaseMapper<Venue> {


    /**
     * 根据商户ID查询场馆数量
     * @param merchantId 商家ID
     * @return 商户的场馆数量
     */
    @Select("SELECT * FROM venues WHERE merchant_id = #{merchantId} ORDER BY created_at DESC")
    Integer countByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 通过 venueId 查询 merchantId
     * @param venueId 场馆ID
     * @return 商户ID
     */
    @Select("SELECT merchant_id FROM venues WHERE venue_id = #{venueId}")
    Long selectMerchantIdByVenueId(@Param("venueId") Long venueId);


    /**
     * 搜索场馆（V2版本 - 数据库层面过滤和排序）
     * 支持多条件筛选：关键词、价格、场地数量、距离等
     */
    List<Map<String, Object>> searchVenues(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minCourtCount") Integer minCourtCount,
            @Param("maxCourtCount") Integer maxCourtCount,
            @Param("userLat") Double userLat,
            @Param("userLng") Double userLng,
            @Param("maxDistance") Double maxDistance,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") Integer sortOrder,
            @Param("facilityVenueIds") List<Long> facilityVenueIds,
            @Param("courtTypeVenueIds") List<Long> courtTypeVenueIds,
            @Param("unavailableVenueIds") List<Long> unavailableVenueIds,
            @Param("offset") int offset,
            @Param("pageSize") int pageSize
    );
    @Select("SELECT COUNT(*) FROM venues WHERE merchant_id = #{merchantId}")
    List<Venue> selectByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 统计搜索场馆总数
     */
    long countSearchVenues(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("minCourtCount") Integer minCourtCount,
            @Param("maxCourtCount") Integer maxCourtCount,
            @Param("userLat") Double userLat,
            @Param("userLng") Double userLng,
            @Param("maxDistance") Double maxDistance,
            @Param("facilityVenueIds") List<Long> facilityVenueIds,
            @Param("courtTypeVenueIds") List<Long> courtTypeVenueIds,
            @Param("unavailableVenueIds") List<Long> unavailableVenueIds
    );

    /**
     * 查询不符合时间可见性规则的场馆ID列表
     * 包括：
     * 1. 未配置maxAdvanceDays的场馆
     * 2. 预订日期超过maxAdvanceDays的场馆
     * 3. 预订今天但还未到slotVisibilityTime的场馆
     *
     * @param bookingDate 预订日期
     * @return 不符合条件的场馆ID列表
     */
    List<Long> selectVenuesViolatingVisibilityRules(@Param("bookingDate") LocalDate bookingDate);

    /**
     * 根据商家ID查询旗下所有场馆ID列表
     * @param merchantId 商家ID
     * @return 场馆ID列表
     */
    @Select("SELECT venue_id FROM venues WHERE merchant_id = #{merchantId} AND status = 1 ORDER BY venue_id")
    List<Long> selectVenueIdsByMerchantId(@Param("merchantId") Long merchantId);

    /**
     * 根据商家ID查询旗下所有场馆（包含完整信息）
     * @param merchantId 商家ID
     * @return 场馆列表
     */
    @Select("SELECT * FROM venues WHERE merchant_id = #{merchantId} AND status = 1 ORDER BY created_at DESC")
    List<Venue> selectVenuesByMerchantId(@Param("merchantId") Long merchantId);

}
