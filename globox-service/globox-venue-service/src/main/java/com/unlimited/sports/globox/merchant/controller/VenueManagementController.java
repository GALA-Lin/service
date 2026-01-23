package com.unlimited.sports.globox.merchant.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.VenueManagementService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.VenueCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.VenueUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueBasicInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

import static com.unlimited.sports.globox.common.result.MerchantErrorCode.NEED_OWNER;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_EMPLOYEE_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * 场馆管理Controller
 * @since 2026-01-15
 */
@Slf4j
@RestController
@RequestMapping("/merchant/venues/management")
@RequiredArgsConstructor
public class VenueManagementController {

    private final VenueManagementService venueManagementService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 创建场馆（支持图片上传）
     *
     * @param employeeId 员工ID
     * @param roleStr 员工角色
     * @param dto 创建场馆DTO
     * @return 场馆详情
     */
    @PostMapping()
    public R<MerchantVenueBasicInfo> createVenue(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid @RequestBody  VenueCreateDto dto) {

        log.info("商家创建场馆 - venueName: {}",
                dto.getName());

        // 认证并获取上下文（只有老板可以创建场馆）
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        if (!context.isOwner()) {
            return R.error(NEED_OWNER);
        }

        MerchantVenueBasicInfo result = venueManagementService.createVenue(
                context.getMerchantId(), dto);

        return R.ok(result);
    }

    /**
     * 更新场馆（支持图片上传）
     *
     * @param employeeId 员工ID
     * @param roleStr 员工角色
     * @param dto 更新场馆DTO
     * @return 场馆详情
     */
    @PutMapping()
    public R<MerchantVenueBasicInfo> updateVenue(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Valid @RequestBody  VenueUpdateDto dto) {

        log.info("商家更新场馆 - venueId: {}",
                dto.getVenueId());

        // 认证并获取上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());

        MerchantVenueBasicInfo result = venueManagementService.updateVenue(
                context.getMerchantId(), dto);

        return R.ok(result);
    }

    /**
     * 删除场馆
     *
     * @param employeeId 员工ID
     * @param roleStr 员工角色
     * @param venueId 场馆ID
     * @return 成功消息
     */
    @DeleteMapping("/{venueId}")
    public R<Void> deleteVenue(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long venueId) {

        log.info("商家删除场馆 - venueId: {}", venueId);

        // 认证并获取上下文（只有老板可以删除场馆）
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        if (!context.isOwner()) {
            return R.error(NEED_OWNER);
        }

        venueManagementService.deleteVenue(context.getMerchantId(), venueId);

        return R.ok();
    }

    /**
     * 切换场馆状态
     *
     * @param employeeId 员工ID
     * @param roleStr 员工角色
     * @param venueId 场馆ID
     * @param status 状态（1-正常，0-暂停营业）
     * @return 场馆详情
     */
    @PostMapping("/{venueId}/toggle-status")
    public R<MerchantVenueBasicInfo> toggleStatus(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long venueId,
            @RequestParam Integer status) {

        log.info("切换场馆状态 - venueId: {}, status: {}", venueId, status);

        // 认证并获取上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, venueId);

        MerchantVenueBasicInfo result = venueManagementService.toggleVenueStatus(
                context.getMerchantId(), venueId, status);

        return R.ok(result);
    }
}