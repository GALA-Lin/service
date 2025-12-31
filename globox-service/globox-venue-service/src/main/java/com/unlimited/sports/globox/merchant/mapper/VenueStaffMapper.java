package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author Linsen Hu
 * @since 2025/12/22 12:14
 * 商家员工 Mapper
 */

@Mapper
public interface VenueStaffMapper extends BaseMapper<VenueStaff> {

    /**
     * 根据用户ID查询在职的员工记录
     *
     * @param userId 用户ID
     * @return 员工记录，如果不存在或已离职则返回null
     */
    @Select("SELECT * FROM venue_staff " +
            "WHERE user_id = #{userId} " +
            "AND status = 1 " +  // status = 1 表示在职
            "LIMIT 1")
    VenueStaff selectActiveStaffByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和场馆ID查询员工记录
     *
     * @param userId  用户ID
     * @param venueId 场馆ID
     * @return 员工记录
     */
    @Select("SELECT * FROM venue_staff " +
            "WHERE user_id = #{userId} " +
            "AND venue_id = #{venueId} " +
            "AND status = 1 " +
            "LIMIT 1")
    VenueStaff selectByUserIdAndVenueId(@Param("userId") Long userId,
                                        @Param("venueId") Long venueId);

    /**
     * 根据用户ID和商家ID查询员工记录
     *
     * @param userId     用户ID
     * @param merchantId 商家ID
     * @return 员工记录
     */
    @Select("SELECT * FROM venue_staff " +
            "WHERE user_id = #{userId} " +
            "AND merchant_id = #{merchantId} " +
            "AND status = 1 " +
            "LIMIT 1")
    VenueStaff selectByUserIdAndMerchantId(@Param("userId") Long userId,
                                           @Param("merchantId") Long merchantId);
}