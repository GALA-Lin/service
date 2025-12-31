package com.unlimited.sports.globox.merchant.controller;


import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.CourtManagementService;
import com.unlimited.sports.globox.model.merchant.dto.CourtCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.CourtUpdateDto;
import com.unlimited.sports.globox.model.merchant.vo.CourtVo;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

/**
 * @author Linsen Hu
 * @since 2025/12/22 14:11
 * 场地管理
 */
@Slf4j
@RestController
@RequestMapping("/merchant/courts")
@RequiredArgsConstructor
public class CourtManagementController {

    private final CourtManagementService courtManagementService;
    private final VenueStaffMapper venueStaffMapper;

    /**
     * 创建场地
     */
    @PostMapping("/create")
    public R<CourtVo> createCourt(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestBody @Validated CourtCreateDto createDTO) {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        CourtVo court = courtManagementService.createCourt(merchantId, createDTO);
        return R.ok(court);
    }

    /**
     * 更新场地
     */
    @PutMapping("/update")
    public R<CourtVo> updateCourt(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestBody @Validated CourtUpdateDto updateDTO) {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        CourtVo court = courtManagementService.updateCourt(merchantId, updateDTO);
        return R.ok(court);
    }

    /**
     * 删除场地
     */
    @DeleteMapping("/{courtId}")
    public R<Long> deleteCourt(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable Long courtId) {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        // todo update status 逻辑删除  DATA: 2025/12/26
        courtManagementService.deleteCourt(merchantId, courtId);
        return R.ok(courtId);
    }

    /**
     * 查询场馆的所有场地
     */
    @GetMapping("/list")
    public R<List<CourtVo>> listCourts(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestParam Long venueId) {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        List<CourtVo> result = courtManagementService.listCourtsByVenue(merchantId, venueId);
        return R.ok(result);
    }

    /**
     * 启用/禁用场地
     */
    @PostMapping("/{courtId}/toggle-status")
    public R<CourtVo> toggleStatus(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable Long courtId,
            @RequestParam Integer status) {
        if (userId == null) {
            log.error("请求头中缺少User-Id,未登录");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }
        Long merchantId = getMerchantIdByUserId(userId);
        CourtVo court = courtManagementService.toggleCourtStatus(merchantId, courtId, status);
        return R.ok(court);
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