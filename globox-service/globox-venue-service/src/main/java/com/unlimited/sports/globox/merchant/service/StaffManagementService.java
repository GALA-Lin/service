package com.unlimited.sports.globox.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.model.merchant.dto.QueryStaffDto;
import com.unlimited.sports.globox.model.merchant.dto.StaffUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.StaffOperationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffVo;

/**
 * 员工管理服务接口
 * @since 2026-01-23
 */
public interface StaffManagementService {

    /**
     * 分页查询商家的员工列表
     *
     * @param merchantId 商家ID
     * @param dto        查询条件
     * @return 员工信息分页列表
     */
    IPage<StaffSimpleVo> queryStaffPage(Long merchantId, QueryStaffDto dto);

    /**
     * 查询员工详细信息
     *
     * @param merchantId   商家ID
     * @param venueStaffId 员工ID
     * @return 员工详细信息
     */
    StaffVo getStaffDetail(Long merchantId, Long venueStaffId);

    /**
     * 统计商家的员工数量
     *
     * @param merchantId 商家ID
     * @param status     员工状态（可选）
     * @return 员工数量
     */
    Integer countStaff(Long merchantId, Integer status);

    /**
     * 更新员工信息
     *
     * @param merchantId 商家ID
     * @param dto        更新信息
     * @return 操作结果
     */
    StaffOperationResultVo updateStaff(Long merchantId, StaffUpdateDto dto);

    /**
     * 删除员工（软删除，设置为离职状态）
     *
     * @param merchantId   商家ID
     * @param venueStaffId 员工ID
     * @return 操作结果
     */
    StaffOperationResultVo deleteStaff(Long merchantId, Long venueStaffId);

    /**
     * 批量删除员工
     *
     * @param merchantId    商家ID
     * @param venueStaffIds 员工ID列表
     * @return 操作结果
     */
    StaffOperationResultVo batchDeleteStaff(Long merchantId, java.util.List<Long> venueStaffIds);

}