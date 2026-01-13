package com.unlimited.sports.globox.merchant.controller;


import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.MerchantMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.service.CourtManagementService;
import com.unlimited.sports.globox.model.merchant.dto.CourtBatchCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.CourtCreateDto;
import com.unlimited.sports.globox.model.merchant.dto.CourtUpdateDto;
import com.unlimited.sports.globox.model.merchant.entity.Merchant;
import com.unlimited.sports.globox.model.merchant.vo.CourtVo;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueBasicInfo;
import com.unlimited.sports.globox.model.merchant.vo.MerchantVenueDetailVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.unlimited.sports.globox.merchant.util.*;

import java.util.List;

import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_EMPLOYEE_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * @author Linsen Hu
 * @since 2025/12/22 14:11
 * 场地管理
 */
@Slf4j
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class CourtManagementController {

    private final CourtManagementService courtManagementService;
    private final MerchantAuthUtil merchantAuthUtil;
    private final VenueMapper venueMapper;

    // 1. 注入 MerchantMapper
    private final MerchantMapper merchantMapper;

    /**
     * 查询当前登录的商家基本信息
     */
    @GetMapping("/info")
    public R<Merchant> getMerchantInfo(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr) {

        // 2. 认证并获取上下文，确保请求合法
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        log.info("[商家信息查询] merchantId: {}", context.getMerchantId());

        // 3. 从数据库中查询商家详细信息
        Merchant merchant = merchantMapper.selectById(context.getMerchantId());

        if (merchant == null) {
            throw new GloboxApplicationException("商家信息不存在");
        }

        return R.ok(merchant);
    }

    /**
     * 创建场地
     */
    @PostMapping("/courts/create")
    public R<CourtVo> createCourt(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Validated CourtCreateDto createDTO) {
        // 认证并获取上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 验证场馆访问权限（员工只能在自己场馆创建场地）
        merchantAuthUtil.validateVenueAccess(context, createDTO.getVenueId());

        CourtVo court = courtManagementService.createCourt(context.getMerchantId(), createDTO);
        return R.ok(court);
    }

    /**
     * 批量创建场地
     */
    @PostMapping("/courts/batch-create")
    public R<List<CourtVo>> batchCreateCourts(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Validated CourtBatchCreateDto batchDTO) {

        // 1. 认证并获取上下文
        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 2. 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, batchDTO.getVenueId());

        // 3. 调用服务层执行批量操作
        List<CourtVo> result = courtManagementService.batchCreateCourts(context.getMerchantId(), batchDTO);
        return R.ok(result);
    }

    /**
     * 更新场地
     */
    @PutMapping("/courts/update")
    public R<CourtVo> updateCourt(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Validated CourtUpdateDto updateDTO) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        CourtVo court = courtManagementService.updateCourt(context.getMerchantId(), updateDTO);
        return R.ok(court);
    }

    /**
     * 删除场地
     */
    @DeleteMapping("/courts/{courtId}")
    public R<Long> deleteCourt(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long courtId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        courtManagementService.deleteCourt(context.getMerchantId(), courtId);
        return R.ok(courtId);
    }

    /**
     * 查询场馆的所有场地
     */
    @GetMapping("/courts/list")
    public R<List<CourtVo>> listCourts(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestParam Long venueId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);
        // 验证场馆访问权限（员工只能查询自己的场馆）
        merchantAuthUtil.validateVenueAccess(context, venueId);
        log.info("[场地查询]参数：merchantId: {}, venueId: {}", context.getMerchantId(), venueId);

        List<CourtVo> result = courtManagementService.listCourtsByVenue(context.getMerchantId(), venueId);
        log.info("[场地查询]成功");
        return R.ok(result);
    }

    /**
     * 启用/禁用场地
     */
    @PostMapping("/courts/{courtId}/toggle-status")
    public R<CourtVo> toggleStatus(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long courtId,
            @RequestParam Integer status) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        CourtVo court = courtManagementService.toggleCourtStatus(context.getMerchantId(), courtId, status);
        return R.ok(court);
    }

    /**
     * 查询商家旗下所有场馆ID
     */
    @GetMapping("/venue/venue-ids")
    public R<List<Long>> getVenueIds(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        List<Long> venueIds;
        log.info("merchantId: {}", context.getMerchantId());
        venueIds = venueMapper.selectVenueIdsByMerchantId(context.getMerchantId());
        log.info("merchantId: {}, venueIds: {}", context.getMerchantId() , venueIds);
        return R.ok(venueIds);
    }

    /**
     * 查询商家旗下所有场馆详细信息
     */
    @GetMapping("/venue/venues-info")
    public R<List<MerchantVenueBasicInfo>> getVenuesInfo(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        log.info("[场馆信息查询] merchantId: {}", context.getMerchantId());

        List<MerchantVenueBasicInfo> venues = courtManagementService.getVenuesByMerchantId(context.getMerchantId());

        log.info("[场馆信息查询] 成功，共查询到 {} 个场馆", venues.size());
        return R.ok(venues);
    }

    /**
     * 查询商家旗下所有场馆及所属场地 (嵌套结构)
     */
    @GetMapping("/venue/venues-with-courts")
    public R<List<MerchantVenueDetailVo>> getVenuesWithCourts(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        log.info("[嵌套信息查询] merchantId: {}", context.getMerchantId());
        List<MerchantVenueDetailVo> result = courtManagementService.getVenuesWithCourts(context.getMerchantId());

        return R.ok(result);
    }

}