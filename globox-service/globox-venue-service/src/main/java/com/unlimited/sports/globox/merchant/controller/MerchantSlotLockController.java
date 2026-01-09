package com.unlimited.sports.globox.merchant.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_EMPLOYEE_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * @since 2025/12/28 11:05
 * 商家锁场管理controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/slots")
@RequiredArgsConstructor
public class MerchantSlotLockController {

    private final MerchantSlotLockService lockService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 锁场
     */
    @PostMapping("/{recordId}/lock")
    public R<Void> lockSlot(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long recordId,
            @RequestParam String reason) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.lockSlotByMerchant(recordId, reason, context.getMerchantId());
        return R.ok();
    }

    /**
     * 批量锁场
     */
    @PostMapping("/batch/lock")
    public R<Void> lockSlotsBatch(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody List<Long> recordIds,
            @RequestParam String reason) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.lockSlotsBatchByMerchant(recordIds, reason, context.getMerchantId());
        return R.ok();
    }

    /**
     * 解锁场
     */
    @PostMapping("/{recordId}/unlock")
    public R<Void> unlockSlot(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long recordId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.unlockSlotByMerchant(recordId, context.getMerchantId());
        return R.ok();
    }

    /**
     * 批量解锁场
     */
    @PostMapping("/batch/unlock")
    public R<Void> unlockSlotsBatch(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody List<Long> recordIds) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.unlockSlotsBatchByMerchant(recordIds, context.getMerchantId());
        return R.ok();
    }

    /**
     * 查询锁场记录
     *
     * @param courtId    场地id
     * @param venueId    场馆id
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param lockedType 锁场类型
     * @param employeeId 员工ID
     * @param roleStr    角色
     * @return 锁场记录列表
     */
    @GetMapping("/locked-slots")
    public R<List<LockedSlotVo>> queryLockedSlots(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) Long venueId,
            @RequestParam @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer lockedType) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 验证场馆访问权限
        if (venueId != null) {
            merchantAuthUtil.validateVenueAccess(context, venueId);
        }

        // 验证场地访问权限
        if (courtId != null) {
            merchantAuthUtil.validateCourtAccess(context, courtId);
        }

        // 如果是员工且未指定场馆，则只查询自己场馆的数据
        if (context.isStaff() && venueId == null) {
            venueId = context.getVenueId();
        }

        List<LockedSlotVo> result = lockService.queryLockedSlots(
                courtId, venueId, startDate, endDate, lockedType);
        return R.ok(result);
    }
}