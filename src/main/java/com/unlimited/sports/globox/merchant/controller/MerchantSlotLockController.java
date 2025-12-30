package com.unlimited.sports.globox.merchant.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
import com.unlimited.sports.globox.model.merchant.dto.SlotLockRequestDto;
import com.unlimited.sports.globox.model.merchant.entity.Court;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import com.unlimited.sports.globox.model.merchant.vo.BatchLockResultVo;
import com.unlimited.sports.globox.model.merchant.vo.BatchUnlockResultVo;
import com.unlimited.sports.globox.model.merchant.vo.LockedSlotVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

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
    private final VenueStaffMapper venueStaffMapper;

    /**
     * 锁场（支持按模板+日期）
     *
     * @param templateId 时段模板ID
     * @param bookingDate 预订日期
     * @param reason 锁场原因
     * @param userId 用户ID
     */
    @PostMapping("/lock")
    public R<Void> lockSlot(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @RequestParam Long templateId,
            @RequestParam @JsonFormat(pattern = "yyyy-MM-dd") LocalDate bookingDate,
            @RequestParam String reason) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        lockService.lockSlotByMerchant(templateId, bookingDate, reason, merchantId);
        return R.ok();
    }

    /**
     * 批量锁场
     * @param requests 锁场请求列表
     * @param reason 锁场原因
     * @param userId 用户ID
     */
    @PostMapping("/batch/lock")
    public R<BatchLockResultVo> lockSlotsBatch(
            @RequestBody List<SlotLockRequestDto> requests,
            @RequestParam String reason,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId) {

        if (userId == null) {
            log.error("请求头中缺少User-Id，未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        BatchLockResultVo result = lockService.lockSlotsBatchByMerchant(requests, reason, merchantId);
        return R.ok(result);
    }

    /**
     * 解锁场地（通过记录ID）
     * @param recordId 时段记录ID
     * @param userId 用户ID
     */
    @PostMapping("/{recordId}/unlock")
    public R<Void> unlockSlot(
            @PathVariable Long recordId,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId) {

        if (userId == null) {
            log.error("请求头中缺少User-Id，未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        lockService.unlockSlotByMerchant(recordId, merchantId);
        return R.ok();
    }

    /**
     * 批量解锁
     * @param recordIds 时段记录ID列表
     * @param userId 用户ID
     */
    @PostMapping("/batch/unlock")
    public R<BatchUnlockResultVo> unlockSlotsBatch(
            @RequestBody List<Long> recordIds,
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId) {

        if (userId == null) {
            log.error("请求头中缺少User-Id，未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        BatchUnlockResultVo result = lockService.unlockSlotsBatchByMerchant(recordIds, merchantId);
        return R.ok(result);
    }
    /**
     * 根据用户ID从商家职工关联表查询merchant_id
     * @param userId 用户ID
     * @return 商家ID
     * @throws GloboxApplicationException 如果用户不是商家员工
     */
    private Long getMerchantIdByUserId(Long userId) {
        // 查询该用户在商家职工关联表中的记录（只查询在职状态）
        VenueStaff venueStaff = venueStaffMapper.selectActiveStaffByUserId(userId);

        if (venueStaff == null) {
            log.error("用户ID: {} 不是任何商家的员工", userId);
            throw new GloboxApplicationException("您不是商家员工，无权访问此资源");
        }

        log.debug("用户ID: {} 对应的商家ID: {}", userId, venueStaff.getMerchantId());
        return venueStaff.getMerchantId();
    }
}