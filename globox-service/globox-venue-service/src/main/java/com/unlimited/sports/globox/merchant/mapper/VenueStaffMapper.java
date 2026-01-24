package com.unlimited.sports.globox.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import com.unlimited.sports.globox.model.merchant.vo.StaffVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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

    /**
     * 分页查询商家的员工列表（带关联信息）
     *
     * @param page         分页对象
     * @param merchantId   商家ID
     * @param venueId      场馆ID（可选）
     * @param status       员工状态（可选）
     * @param roleType     角色类型（可选）
     * @param displayName  员工名称（可选，模糊查询）
     * @param jobTitle     职位名称（可选，模糊查询）
     * @param employeeNo   工号（可选，精确查询）
     * @return 员工信息分页列表
     */
    IPage<StaffVo> selectStaffPage(
            Page<?> page,
            @Param("merchantId") Long merchantId,
            @Param("venueId") Long venueId,
            @Param("status") Integer status,
            @Param("roleType") Integer roleType,
            @Param("displayName") String displayName,
            @Param("jobTitle") String jobTitle,
            @Param("employeeNo") String employeeNo
    );

    /**
     * 根据员工ID查询详细信息（包含关联数据）
     *
     * @param venueStaffId 员工ID
     * @return 员工详细信息
     */
    StaffVo selectStaffDetail(@Param("venueStaffId") Long venueStaffId);

    /**
     * 统计商家的员工数量
     *
     * @param merchantId 商家ID
     * @param status     员工状态（可选）
     * @return 员工数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM venue_staff " +
            "WHERE merchant_id = #{merchantId} " +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "</script>")
    Integer countStaffByMerchant(
            @Param("merchantId") Long merchantId,
            @Param("status") Integer status
    );

    /**
     * 查询场馆的所有员工（不分页）
     *
     * @param venueId 场馆ID
     * @param status  员工状态（可选）
     * @return 员工列表
     */
    @Select("<script>" +
            "SELECT * FROM venue_staff " +
            "WHERE venue_id = #{venueId} " +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY created_at DESC" +
            "</script>")
    List<VenueStaff> selectByVenueId(
            @Param("venueId") Long venueId,
            @Param("status") Integer status
    );

    /**
     * 检查工号是否已存在
     *
     * @param merchantId  商家ID
     * @param employeeNo  工号
     * @param excludeId   排除的员工ID（更新时使用）
     * @return 存在的员工数量
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM venue_staff " +
            "WHERE merchant_id = #{merchantId} " +
            "AND employee_no = #{employeeNo} " +
            "<if test='excludeId != null'>" +
            "AND venue_staff_id != #{excludeId} " +
            "</if>" +
            "</script>")
    Integer checkEmployeeNoExists(
            @Param("merchantId") Long merchantId,
            @Param("employeeNo") String employeeNo,
            @Param("excludeId") Long excludeId
    );

    /**
     * 检查用户是否已是该商家的员工
     *
     * @param merchantId 商家ID
     * @param userId     用户ID
     * @return 存在的员工数量
     */
    @Select("SELECT COUNT(*) FROM venue_staff " +
            "WHERE merchant_id = #{merchantId} " +
            "AND user_id = #{userId} " +
            "AND status != 0")  // 排除已离职的
    Integer checkUserIsStaff(
            @Param("merchantId") Long merchantId,
            @Param("userId") Long userId
    );
}