package com.unlimited.sports.globox.merchant.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.LockSlotRequest;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * @since 2025/12/28 11:05
 * 商家锁场管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/slots")
@RequiredArgsConstructor
public class MerchantSlotLockController {

    private final MerchantSlotLockService lockService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 锁场（支持单个和批量）
     */
    @PostMapping("/batch/lock")
    public R<Void> lockSlots(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Valid @RequestBody LockSlotRequest request) {

        log.info("锁场请求, employeeId: {}, roleStr: {}, request: {}",
                employeeId, roleStr, request);

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        // 根据模板ID列表数量决定是单个锁场还是批量锁场
        if (request.getTemplateIds().size() == 1) {
            // 单个锁场
            lockService.lockSlotByMerchant(
                    request.getTemplateIds().get(0),
                    request.getBookingDate(),
                    request.getReason(),
                    request.getUserName(),
                    request.getUserPhone(),
                    employeeId,
                    context.getMerchantId()
            );
        } else {
            // 批量锁场（会生成批次ID）
            lockService.lockSlotsBatchByMerchant(
                    request.getTemplateIds(),
                    request.getBookingDate(),
                    request.getReason(),
                    request.getUserName(),
                    request.getUserPhone(),
                    employeeId,
                    context.getMerchantId()
            );
        }

        return R.ok();
    }

    /**
     * 解锁场（支持单个和批量）
     */
    @PostMapping("/batch/unlock")
    public R<Void> unlockSlots(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Valid @RequestBody UnlockSlotRequest request) {

        log.info("解锁场请求, employeeId: {}, roleStr: {}, request: {}",
                employeeId, roleStr, request);

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        // 根据模板ID列表数量决定是单个解锁还是批量解锁
        if (request.getTemplateIds().size() == 1) {
            // 单个解锁
            lockService.unlockSlotByMerchant(
                    request.getTemplateIds().get(0),
                    request.getBookingDate(),
                    context.getMerchantId()
            );
        } else {
            // 批量解锁
            lockService.unlockSlotsBatchByMerchant(
                    request.getTemplateIds(),
                    request.getBookingDate(),
                    context.getMerchantId()
            );
        }

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
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) Long venueId,
            @RequestParam @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer lockedType) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

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
     * 解锁请求DTO
     */
    @Data
    public static class UnlockSlotRequest {
        @NotEmpty(message = "模板ID列表不能为空")
        @Size(max = 100, message = "一次最多解锁100个时段")
        private List<Long> templateIds;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate bookingDate;
    }
}