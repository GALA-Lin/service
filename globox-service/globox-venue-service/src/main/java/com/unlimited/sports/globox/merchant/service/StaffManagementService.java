package com.unlimited.sports.globox.merchant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.model.merchant.dto.QueryStaffDto;
import com.unlimited.sports.globox.model.merchant.dto.StaffSelfUpdateDto;
import com.unlimited.sports.globox.model.merchant.dto.StaffUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.StaffOperationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffVo;

import java.util.List;

/**
 * 员工管理服务接口
 * @since 2026-01-23
 */
public interface StaffManagementService {

    /**
     * 分页查询商家员工列表
     */
    IPage<StaffSimpleVo> queryStaffPage(Long merchantId, QueryStaffDto dto);

    /**
     * 查询员工详细信息
     */
    StaffVo getStaffDetail(Long merchantId, Long venueStaffId);

    /**
     * 统计商家员工数量
     */
    Integer countStaff(Long merchantId, Integer status);

    /**
     * 更新员工信息（OWNER权限）
     * 场馆OWNER可以修改场馆内所有员工信息
     */
    StaffOperationResultVo updateStaff(Long merchantId, Long operatorUserId, StaffUpdateDto dto);

    /**
     * 员工自助修改信息
     * 员工只能修改自己的部分信息
     */
    StaffOperationResultVo updateSelfInfo(Long userId, StaffSelfUpdateDto dto);

    /**
     * 删除员工（软删除）
     */
    StaffOperationResultVo deleteStaff(Long merchantId, Long venueStaffId);

    /**
     * 批量删除员工
     */
    StaffOperationResultVo batchDeleteStaff(Long merchantId, List<Long> venueStaffIds);
}