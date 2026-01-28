package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.StaffManagementService;
import com.unlimited.sports.globox.model.merchant.dto.QueryStaffDto;
import com.unlimited.sports.globox.model.merchant.dto.StaffSelfUpdateDto;
import com.unlimited.sports.globox.model.merchant.dto.StaffUpdateDto;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import com.unlimited.sports.globox.model.merchant.vo.StaffOperationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工管理服务实现类
 * @since 2026-01-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffManagementServiceImpl implements StaffManagementService {

    private final VenueStaffMapper venueStaffMapper;
    private final VenueMapper venueMapper;

    @Override
    public IPage<StaffSimpleVo> queryStaffPage(Long merchantId, QueryStaffDto dto) {
        log.info("分页查询商家员工列表 - merchantId: {}, dto: {}", merchantId, dto);

        // 构建分页对象
        Page<StaffVo> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        // 查询员工列表
        IPage<StaffVo> staffPage = venueStaffMapper.selectStaffPage(
                page,
                merchantId,
                dto.getVenueId(),
                dto.getStatus(),
                dto.getRoleType(),
                dto.getDisplayName(),
                dto.getJobTitle(),
                dto.getEmployeeNo()
        );

        // 转换为简要信息VO
        IPage<StaffSimpleVo> result = staffPage.convert(this::convertToSimpleVo);

        log.info("查询完成 - 总记录数: {}, 当前页记录数: {}",
                result.getTotal(), result.getRecords().size());

        return result;
    }

    @Override
    public StaffVo getStaffDetail(Long merchantId, Long venueStaffId) {
        log.info("查询员工详细信息 - merchantId: {}, venueStaffId: {}",
                merchantId, venueStaffId);

        StaffVo staffVo = venueStaffMapper.selectStaffDetail(venueStaffId);

        if (staffVo == null) {
            log.warn("员工不存在 - venueStaffId: {}", venueStaffId);
            throw new GloboxApplicationException("员工信息不存在");
        }

        // 验证员工是否属于该商家
        if (!staffVo.getMerchantId().equals(merchantId)) {
            log.error("无权查看该员工信息 - merchantId: {}, venueStaffId: {}, staffMerchantId: {}",
                    merchantId, venueStaffId, staffVo.getMerchantId());
            throw new GloboxApplicationException("无权查看该员工信息");
        }

        log.info("查询成功 - 员工: {}, 职位: {}",
                staffVo.getDisplayName(), staffVo.getJobTitle());

        return staffVo;
    }

    @Override
    public Integer countStaff(Long merchantId, Integer status) {
        log.info("统计商家员工数量 - merchantId: {}, status: {}", merchantId, status);

        Integer count = venueStaffMapper.countStaffByMerchant(merchantId, status);

        log.info("统计完成 - 员工数量: {}", count);

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StaffOperationResultVo updateStaff(Long merchantId, Long operatorUserId, StaffUpdateDto dto) {
        log.info("更新员工信息(管理员) - merchantId: {}, operatorUserId: {}, dto: {}",
                merchantId, operatorUserId, dto);

        // 1. 查询要修改的员工信息
        VenueStaff targetStaff = venueStaffMapper.selectById(dto.getVenueStaffId());
        if (targetStaff == null) {
            log.warn("员工不存在 - venueStaffId: {}", dto.getVenueStaffId());
            throw new GloboxApplicationException("员工信息不存在");
        }

        // 2. 验证员工是否属于该商家
        if (!targetStaff.getMerchantId().equals(merchantId)) {
            log.error("员工不属于该商家 - merchantId: {}, venueStaffId: {}, staffMerchantId: {}",
                    merchantId, dto.getVenueStaffId(), targetStaff.getMerchantId());
            throw new GloboxApplicationException("无权操作该员工");
        }

        // 3. 查询操作人信息，验证权限
        VenueStaff operator = venueStaffMapper.selectActiveStaffByUserId(operatorUserId);
        if (operator == null) {
            log.error("操作人不是有效员工 - userId: {}", operatorUserId);
            throw new GloboxApplicationException("无权限操作");
        }

        // 4. 权限验证：只有场馆OWNER可以修改其他员工信息
        if (operator.getRoleType() != 1) { // 1=OWNER
            log.error("操作人不是OWNER，无权修改其他员工信息 - operatorId: {}, roleType: {}",
                    operator.getVenueStaffId(), operator.getRoleType());
            throw new GloboxApplicationException("只有场馆负责人可以修改员工信息");
        }

        // 5. 验证操作人和目标员工在同一场馆
        if (!operator.getVenueId().equals(targetStaff.getVenueId())) {
            log.error("操作人和目标员工不在同一场馆 - operatorVenueId: {}, targetVenueId: {}",
                    operator.getVenueId(), targetStaff.getVenueId());
            throw new GloboxApplicationException("只能管理同一场馆的员工");
        }

        // 6. 如果修改了场馆，验证新场馆是否属于该商家
        if (dto.getVenueId() != null && !dto.getVenueId().equals(targetStaff.getVenueId())) {
            Venue venue = venueMapper.selectById(dto.getVenueId());
            if (venue == null || !venue.getMerchantId().equals(merchantId)) {
                log.error("场馆不存在或不属于该商家 - venueId: {}, merchantId: {}",
                        dto.getVenueId(), merchantId);
                throw new GloboxApplicationException("场馆不存在或无权操作");
            }
        }

        // 7. 如果修改了工号，检查是否重复
        if (dto.getEmployeeNo() != null && !dto.getEmployeeNo().isEmpty()) {
            Integer employeeNoCount = venueStaffMapper.checkEmployeeNoExists(
                    merchantId, dto.getEmployeeNo(), dto.getVenueStaffId());
            if (employeeNoCount > 0) {
                log.warn("工号已存在 - employeeNo: {}, merchantId: {}",
                        dto.getEmployeeNo(), merchantId);
                throw new GloboxApplicationException("工号已存在");
            }
        }

        // 8. 更新员工信息
        LambdaUpdateWrapper<VenueStaff> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VenueStaff::getVenueStaffId, dto.getVenueStaffId());

        if (dto.getVenueId() != null) {
            updateWrapper.set(VenueStaff::getVenueId, dto.getVenueId());
        }
        if (dto.getRoleType() != null) {
            updateWrapper.set(VenueStaff::getRoleType, dto.getRoleType());
        }
        if (dto.getDisplayName() != null) {
            updateWrapper.set(VenueStaff::getDisplayName, dto.getDisplayName());
        }
        if (dto.getContactPhone() != null) {
            updateWrapper.set(VenueStaff::getContactPhone, dto.getContactPhone());
        }
        if (dto.getEmail() != null) {
            updateWrapper.set(VenueStaff::getEmail, dto.getEmail());
        }
        if (dto.getJobTitle() != null) {
            updateWrapper.set(VenueStaff::getJobTitle, dto.getJobTitle());
        }
        if (dto.getEmployeeNo() != null) {
            updateWrapper.set(VenueStaff::getEmployeeNo, dto.getEmployeeNo());
        }
        if (dto.getStatus() != null) {
            updateWrapper.set(VenueStaff::getStatus, dto.getStatus());
            // 如果设置为离职状态，自动设置离职时间
            if (dto.getStatus() == 0 && dto.getResignedAt() == null) {
                updateWrapper.set(VenueStaff::getResignedAt, LocalDateTime.now());
            }
        }
        if (dto.getHiredAt() != null) {
            updateWrapper.set(VenueStaff::getHiredAt, dto.getHiredAt());
        }
        if (dto.getResignedAt() != null) {
            updateWrapper.set(VenueStaff::getResignedAt, dto.getResignedAt());
        }
        if (dto.getPermissions() != null) {
            updateWrapper.set(VenueStaff::getPermissions, dto.getPermissions());
        }
        if (dto.getRemark() != null) {
            updateWrapper.set(VenueStaff::getRemark, dto.getRemark());
        }

        // 9. 执行更新
        int updateCount = venueStaffMapper.update(null, updateWrapper);
        if (updateCount <= 0) {
            log.error("更新员工失败 - venueStaffId: {}", dto.getVenueStaffId());
            throw new GloboxApplicationException("更新员工信息失败");
        }

        log.info("更新员工成功 - venueStaffId: {}, displayName: {}, operator: {}",
                targetStaff.getVenueStaffId(),
                dto.getDisplayName() != null ? dto.getDisplayName() : targetStaff.getDisplayName(),
                operator.getDisplayName());

        return StaffOperationResultVo.success(
                targetStaff.getVenueStaffId(),
                dto.getDisplayName() != null ? dto.getDisplayName() : targetStaff.getDisplayName(),
                "UPDATE",
                "员工信息更新成功"
        );
    }

    /**
     * 员工自助修改信息
     * 员工只能修改自己的部分信息
     *
     * @param userId
     * @param dto
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StaffOperationResultVo updateSelfInfo(Long userId, StaffSelfUpdateDto dto) {
        log.info("员工自助修改信息 - userId: {}, dto: {}", userId, dto);

        // 1. 查询当前用户的员工信息
        VenueStaff staff = venueStaffMapper.selectActiveStaffByUserId(userId);
        if (staff == null) {
            log.warn("用户不是员工或已离职 - userId: {}", userId);
            throw new GloboxApplicationException("您不是有效的员工用户");
        }

        // 2. 更新员工信息（只能修改部分字段）
        LambdaUpdateWrapper<VenueStaff> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VenueStaff::getVenueStaffId, staff.getVenueStaffId());

        boolean hasUpdate = false;

        if (dto.getDisplayName() != null && !dto.getDisplayName().isEmpty()) {
            updateWrapper.set(VenueStaff::getDisplayName, dto.getDisplayName());
            hasUpdate = true;
        }
        if (dto.getContactPhone() != null && !dto.getContactPhone().isEmpty()) {
            updateWrapper.set(VenueStaff::getContactPhone, dto.getContactPhone());
            hasUpdate = true;
        }
        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            updateWrapper.set(VenueStaff::getEmail, dto.getEmail());
            hasUpdate = true;
        }

        if (!hasUpdate) {
            log.warn("没有需要更新的字段 - userId: {}", userId);
            throw new GloboxApplicationException("请至少修改一项信息");
        }

        // 3. 执行更新
        int updateCount = venueStaffMapper.update(null, updateWrapper);
        if (updateCount <= 0) {
            log.error("更新员工信息失败 - venueStaffId: {}", staff.getVenueStaffId());
            throw new GloboxApplicationException("更新信息失败");
        }

        log.info("员工自助修改成功 - venueStaffId: {}, displayName: {}",
                staff.getVenueStaffId(), staff.getDisplayName());

        return StaffOperationResultVo.success(
                staff.getVenueStaffId(),
                dto.getDisplayName() != null ? dto.getDisplayName() : staff.getDisplayName(),
                "SELF_UPDATE",
                "个人信息更新成功"
        );
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StaffOperationResultVo deleteStaff(Long merchantId, Long venueStaffId) {
        log.info("删除员工 - merchantId: {}, venueStaffId: {}", merchantId, venueStaffId);

        // 1. 查询员工信息
        VenueStaff staff = venueStaffMapper.selectById(venueStaffId);
        if (staff == null) {
            log.warn("员工不存在 - venueStaffId: {}", venueStaffId);
            throw new GloboxApplicationException("员工信息不存在");
        }

        // 2. 验证员工是否属于该商家
        if (!staff.getMerchantId().equals(merchantId)) {
            log.error("无权删除该员工 - merchantId: {}, venueStaffId: {}, staffMerchantId: {}",
                    merchantId, venueStaffId, staff.getMerchantId());
            throw new GloboxApplicationException("无权删除该员工");
        }

        // 3. 软删除：设置为离职状态
        LambdaUpdateWrapper<VenueStaff> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(VenueStaff::getVenueStaffId, venueStaffId)
                .set(VenueStaff::getStatus, 0) // 设置为离职
                .set(VenueStaff::getResignedAt, LocalDateTime.now());

        int updateCount = venueStaffMapper.update(null, updateWrapper);
        if (updateCount <= 0) {
            log.error("删除员工失败 - venueStaffId: {}", venueStaffId);
            throw new GloboxApplicationException("删除员工失败");
        }

        log.info("删除员工成功 - venueStaffId: {}, displayName: {}",
                staff.getVenueStaffId(), staff.getDisplayName());

        return StaffOperationResultVo.success(
                staff.getVenueStaffId(),
                staff.getDisplayName(),
                "DELETE",
                "员工已设置为离职状态"
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StaffOperationResultVo batchDeleteStaff(Long merchantId, List<Long> venueStaffIds) {
        log.info("批量删除员工 - merchantId: {}, venueStaffIds: {}", merchantId, venueStaffIds);

        if (venueStaffIds == null || venueStaffIds.isEmpty()) {
            throw new GloboxApplicationException("员工ID列表不能为空");
        }

        int successCount = 0;
        int failCount = 0;

        for (Long venueStaffId : venueStaffIds) {
            try {
                deleteStaff(merchantId, venueStaffId);
                successCount++;
            } catch (Exception e) {
                log.warn("删除员工失败 - venueStaffId: {}, error: {}",
                        venueStaffId, e.getMessage());
                failCount++;
            }
        }

        String message = String.format("批量删除完成，成功: %d, 失败: %d", successCount, failCount);
        log.info(message);

        return StaffOperationResultVo.builder()
                .success(failCount == 0)
                .operationType("BATCH_DELETE")
                .message(message)
                .build();
    }

    /**
     * 转换为简要信息VO
     */
    private StaffSimpleVo convertToSimpleVo(StaffVo staffVo) {
        return StaffSimpleVo.builder()
                .venueStaffId(staffVo.getVenueStaffId())
                .userId(staffVo.getUserId())
                .venueId(staffVo.getVenueId())
                .venueName(staffVo.getVenueName())
                .displayName(staffVo.getDisplayName())
                .contactPhone(staffVo.getContactPhone())
                .email(staffVo.getEmail())
                .jobTitle(staffVo.getJobTitle())
                .employeeNo(staffVo.getEmployeeNo())
                .roleTypeName(staffVo.getRoleTypeName())
                .status(staffVo.getStatus())
                .statusName(staffVo.getStatusName())
                .hiredAt(staffVo.getHiredAt())
                .build();
    }

}