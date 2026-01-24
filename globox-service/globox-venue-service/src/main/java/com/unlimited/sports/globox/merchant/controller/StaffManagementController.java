package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.StaffManagementService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.QueryStaffDto;
import com.unlimited.sports.globox.model.merchant.dto.StaffUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.StaffOperationResultVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.StaffVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import java.util.List;

import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_EMPLOYEE_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * 员工管理Controller
 * @since 2026-01-23
 */
@Slf4j
@RestController
@RequestMapping("/merchant/staff")
@RequiredArgsConstructor
public class StaffManagementController {

    private final StaffManagementService staffManagementService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 分页查询员工列表
     *
     * @param employeeId 员工ID（请求头）
     * @param roleStr    角色字符串（请求头）
     * @param dto        查询条件
     * @return 员工信息分页列表
     */
    @GetMapping("/list")
    public R<IPage<StaffSimpleVo>> queryStaffList(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid QueryStaffDto dto) {

        log.info("查询员工列表 - employeeId: {}, roleStr: {}, dto: {}",
                employeeId, roleStr, dto);

        // 验证权限并获取认证上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 只有商家所有者可以查询员工列表
        merchantAuthUtil.requireOwner(context);

        // 如果指定了场馆ID，验证场馆访问权限
        if (dto.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());
        }

        // 查询员工列表
        IPage<StaffSimpleVo> result = staffManagementService.queryStaffPage(
                context.getMerchantId(), dto);

        return R.ok(result);
    }

    /**
     * 查询员工详细信息
     *
     * @param employeeId   员工ID（请求头）
     * @param roleStr      角色字符串（请求头）
     * @param venueStaffId 要查询的员工ID
     * @return 员工详细信息
     */
    @GetMapping("/detail/{venueStaffId}")
    public R<StaffVo> getStaffDetail(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long venueStaffId) {

        log.info("查询员工详细信息 - employeeId: {}, roleStr: {}, venueStaffId: {}",
                employeeId, roleStr, venueStaffId);

        // 验证权限并获取认证上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 只有商家所有者可以查询员工详情
        merchantAuthUtil.requireOwner(context);

        // 查询员工详细信息
        StaffVo result = staffManagementService.getStaffDetail(
                context.getMerchantId(), venueStaffId);

        return R.ok(result);
    }

    /**
     * 统计员工数量
     *
     * @param employeeId 员工ID（请求头）
     * @param roleStr    角色字符串（请求头）
     * @param status     员工状态（可选）
     * @return 员工数量
     */
    @GetMapping("/count")
    public R<Integer> countStaff(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam(required = false) Integer status) {

        log.info("统计员工数量 - employeeId: {}, roleStr: {}, status: {}",
                employeeId, roleStr, status);

        // 验证权限并获取认证上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 只有商家所有者可以统计员工数量
        merchantAuthUtil.requireOwner(context);

        // 统计员工数量
        Integer count = staffManagementService.countStaff(context.getMerchantId(), status);

        return R.ok(count);
    }
    /**
    更新员工信息

    @param employeeId 员工ID（请求头）
    @param roleStr    角色字符串（请求头）
    @param dto        更新信息
    @return 操作结果
    */
    @PutMapping("/update")
    public R<StaffOperationResultVo> updateStaff(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid @RequestBody StaffUpdateDto dto) {
        log.info("更新员工信息 - employeeId: {}, roleStr: {}, dto: {}",
                employeeId, roleStr, dto);
        // 验证权限并获取认证上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        // 只有商家所有者可以更新员工信息
        merchantAuthUtil.requireOwner(context);
        // 如果修改了场馆，验证场馆访问权限
        if (dto.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());
        }
        // 更新员工信息
        StaffOperationResultVo result = staffManagementService.updateStaff(
                context.getMerchantId(), dto);
        return R.ok(result);
    }
    /**
     删除员工（软删除）设置为离职状态

     @param employeeId   员工ID（请求头）
     @param roleStr      角色字符串（请求头）
     @param venueStaffId 要删除的员工ID
     @return 操作结果
     */
    @DeleteMapping("/delete/{venueStaffId}")
    public R<StaffOperationResultVo> deleteStaff(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long venueStaffId) {
        log.info("删除员工 - employeeId: {}, roleStr: {}, venueStaffId: {}",
                employeeId, roleStr, venueStaffId);
        // 验证权限并获取认证上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        // 只有商家所有者可以删除员工
        merchantAuthUtil.requireOwner(context);
        // 删除员工
        StaffOperationResultVo result = staffManagementService.deleteStaff(
                context.getMerchantId(), venueStaffId);
        return R.ok(result);
    }

    /**
     批量删除员工，设置为离职状态

     @param employeeId    员工ID（请求头）
     @param roleStr       角色字符串（请求头）
     @param venueStaffIds 要删除的员工ID列表
     @return 操作结果
     */
    @DeleteMapping("/batch-delete")
    public R<StaffOperationResultVo> batchDeleteStaff(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody List<Long> venueStaffIds) {
        log.info("批量删除员工 - employeeId: {}, roleStr: {}, venueStaffIds: {}",
                employeeId, roleStr, venueStaffIds);
        // 验证权限并获取认证上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        // 只有商家所有者可以批量删除员工
        merchantAuthUtil.requireOwner(context);
        // 批量删除员工
        StaffOperationResultVo result = staffManagementService.batchDeleteStaff(
                context.getMerchantId(), venueStaffIds);
        return R.ok(result);
    }
}