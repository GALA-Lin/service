package com.unlimited.sports.globox.merchant.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.CourtMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.MerchantSlotLockService;
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
    private final VenueMapper venueMapper;
    private final CourtMapper courtMapper;
    private final VenueStaffMapper venueStaffMapper;

    /**
     * 锁场
     *
     * @param recordId 时段记录id
     * @param reason 锁场原因
     * @param userId 用户id
     *
     */
    @PostMapping("/{recordId}/lock")
    public R<Void> lockSlot(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable Long recordId,
            @RequestParam String reason ){
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        lockService.lockSlotByMerchant(recordId, reason, merchantId);
        return R.ok();
    }

    /**
     * 批量锁场
     * @param recordIds 时段记录id列表
     * @param reason 锁场原因
     * @param userId 用户id
     */
    @PostMapping("/batch/lock")
    public R<Void> lockSlotsBatch(
            @RequestBody List<Long> recordIds,
            @RequestParam String reason,
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId)  {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        lockService.lockSlotsBatchByMerchant(recordIds, reason, merchantId);
        return R.ok();
    }

    /**
     * 解锁场
     * @param recordId 时段记录id
     * @param userId 用户id
     */
    @PostMapping("/{recordId}/unlock")
    public R<Void> unlockSlot(
            @PathVariable Long recordId,
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId) {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        lockService.unlockSlotByMerchant(recordId, merchantId);
        return R.ok();
    }

    /**
     * 批量解锁场
     * @param recordIds 时段记录id列表
     * @param userId     用户id
     */
    @PostMapping("/batch/unlock")
    public R<Void> unlockSlotsBatch(
            @RequestBody List<Long> recordIds,
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId) {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        lockService.unlockSlotsBatchByMerchant(recordIds, merchantId);
        return R.ok();
    }

    /**
     * 查询锁场记录
     * @param courtId 场地id
     * @param venueId 场馆id
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param lockedType 锁场类型
     * @param userId 用户id
     * @return 锁场记录列表
     */
    @GetMapping("/locked-slots")
    public R<List<LockedSlotVo>> queryLockedSlots(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestParam(required = false) Long courtId,
            @RequestParam(required = false) Long venueId,
            @RequestParam @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Integer lockedType) {

        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        // 权限校验（确保商家只能查询自己的场馆）
        if (venueId != null) {
            Venue venue = venueMapper.selectById(venueId);
            if (venue == null || !venue.getMerchantId().equals(merchantId)) {
                throw new GloboxApplicationException("无权查询该场馆");
            }
        }

        if (courtId != null) {
            Court court = courtMapper.selectById(courtId);
            Venue venue = venueMapper.selectById(court.getVenueId());
            if (venue == null || !venue.getMerchantId().equals(merchantId)) {
                throw new GloboxApplicationException("无权查询该场地");
            }
        }

        List<LockedSlotVo> result = lockService.queryLockedSlots(
                courtId, venueId, startDate, endDate, lockedType);
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