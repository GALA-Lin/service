package com.unlimited.sports.globox.merchant.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

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
    @PostMapping("/lock")
    public R<Void> lockSlot(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam @NotNull(message = "模板ID不能为空") Long templateId,
            @RequestParam @NotNull(message = "预约日期不能为空")
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @RequestParam @NotBlank(message = "锁定原因不能为空") String reason) {
        log.info("锁场请求, employeeId: {}, roleStr: {}, templateId: {}, bookingDate: {}, reason: {}",
                employeeId, roleStr, templateId, bookingDate, reason);
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.lockSlotByMerchant(templateId, bookingDate, reason, context.getMerchantId());
        return R.ok();
    }

    /**
     * 批量锁场
     */
    @PostMapping("/batch/lock")
    public R<Void> lockSlotsBatch(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam @NotNull(message = "预约日期不能为空")
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @RequestParam @NotBlank(message = "锁定原因不能为空") String reason,
            @Valid @RequestBody BatchLockRequest request) {
        log.info("批量锁场请求, employeeId: {}, roleStr: {}, bookingDate: {}, reason: {}, request: {}",
                employeeId, roleStr, bookingDate, reason, request);
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.lockSlotsBatchByMerchant(
                request.getTemplateIds(), bookingDate, reason, context.getMerchantId());
        return R.ok();
    }

    /**
     * 解锁场
     */
    @PostMapping("/unlock")
    public R<Void> unlockSlot(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam @NotNull(message = "模板ID不能为空") Long templateId,
            @RequestParam @NotNull(message = "预约日期不能为空")
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate) {

        log.info("解锁场请求, employeeId: {}, roleStr: {}, templateId: {}, bookingDate: {}",
                employeeId, roleStr, templateId, bookingDate);
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.unlockSlotByMerchant(templateId, bookingDate, context.getMerchantId());
        return R.ok();
    }

    /**
     * 批量解锁场
     */
    @PostMapping("/batch/unlock")
    public R<Void> unlockSlotsBatch(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam @NotNull(message = "预约日期不能为空")
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @Valid @RequestBody BatchUnlockRequest request) {

        log.info("批量解锁场请求, employeeId: {}, roleStr: {}, bookingDate: {}, request: {}",
                employeeId, roleStr, bookingDate, request);
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        lockService.unlockSlotsBatchByMerchant(
                request.getTemplateIds(), bookingDate, context.getMerchantId());
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

    /**
     * 批量锁场请求
     */
    @Data
    public static class BatchLockRequest {
        @NotEmpty(message = "模板ID列表不能为空")
        @Size(max = 100, message = "一次最多锁定100个时段")
        private List<Long> templateIds;
    }

    /**
     * 批量解锁场请求
     */
    @Data
    public static class BatchUnlockRequest {
        @NotEmpty(message = "模板ID列表不能为空")
        @Size(max = 100, message = "一次最多解锁100个时段")
        private List<Long> templateIds;
    }
}