package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.RefundRuleService;
import com.unlimited.sports.globox.model.merchant.dto.BindRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.dto.CreateRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.dto.QueryRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.dto.UpdateRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

/**
 * @since 2025/12/31 12:05
 * 退款规则管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/merchant/refund-rules")
@RequiredArgsConstructor
public class RefundRuleController {

    private final RefundRuleService refundRuleService;
    private final VenueStaffMapper venueStaffMapper;

    /**
     * 创建退款规则
     * @param userId 用户ID
     * @param dto 创建退款规则DTO
     * @return 创建的退款规则VO
     */
    @PostMapping
    public R<RefundRuleVo> createRefundRule(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @RequestBody @Validated CreateRefundRuleDto dto) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        RefundRuleVo result = refundRuleService.createRefundRule(merchantId, dto);
        return R.ok(result);
    }

    /**
     * 更新退款规则
     * @param userId 用户ID
     * @param dto 更新退款规则DTO
     * @return 更新后的退款规则VO
     */
    @PutMapping
    public R<RefundRuleVo> updateRefundRule(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @RequestBody @Validated UpdateRefundRuleDto dto) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        RefundRuleVo result = refundRuleService.updateRefundRule(merchantId, dto);
        return R.ok(result);
    }

    /**
     * 删除退款规则
     * 删除明细 & 删除规则（逻辑删除）
     * @param userId 用户ID
     * @param ruleId 退款规则ID
     * @return 删除的规则ID
     */
    @DeleteMapping("/{ruleId}")
    public R<Long> deleteRefundRule(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @PathVariable Long ruleId) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        refundRuleService.deleteRefundRule(merchantId, ruleId);
        return R.ok(ruleId);
    }

    /**
     * 获取退款规则详情
     * @param userId 用户ID
     * @param ruleId 退款规则ID
     * @return 退款规则详情VO
     */
    @GetMapping("/{ruleId}")
    public R<RefundRuleVo> getRefundRule(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @PathVariable Long ruleId) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        RefundRuleVo result = refundRuleService.getRefundRule(merchantId, ruleId);
        return R.ok(result);
    }

    /**
     * 分页查询退款规则列表
     * @param userId 用户ID
     * @param dto 查询条件DTO
     * @return 退款规则分页列表VO
     */
    @GetMapping
    public R<Page<RefundRuleSimpleVo>> queryRefundRules(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @Validated QueryRefundRuleDto dto) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        Page<RefundRuleSimpleVo> result = refundRuleService.queryRefundRules(merchantId, dto);
        return R.ok(result);
    }

    /**
     * 绑定退款规则到场馆
     * @param userId 用户ID
     * @param dto 绑定退款规则DTO
     * @return 绑定结果
     */
    @PostMapping("/bind")
    public R<Void> bindRefundRule(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @RequestBody @Validated BindRefundRuleDto dto) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        refundRuleService.bindRefundRule(merchantId, dto);
        return R.ok();
    }

    /**
     * 设置默认退款规则
     * @param userId 用户ID
     * @param ruleId 规则ID
     * @return 设置结果
     */
    @PostMapping("/{ruleId}/set-default")
    public R<Void> setDefaultRule(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @PathVariable Long ruleId) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        refundRuleService.setDefaultRule(merchantId, ruleId);
        return R.ok();
    }

    /**
     * 启用/禁用退款规则
     * @param userId 用户ID
     * @param ruleId 规则ID
     * @param enabled 是否启用
     * @return 启用/禁用
     */
    @PostMapping("/{ruleId}/toggle-status")
    public R<Void> toggleRuleStatus(
            @RequestHeader(value = HEADER_USER_ID, required = false) Long userId,
            @PathVariable Long ruleId,
            @RequestParam Boolean enabled) {

        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        refundRuleService.toggleRuleStatus(merchantId, ruleId, enabled);
        return R.ok();
    }


    /**
     * 根据用户ID从商家职工关联表查询merchant_id
     */
    private Long getMerchantIdByUserId(Long userId) {
        VenueStaff venueStaff = venueStaffMapper.selectActiveStaffByUserId(userId);

        if (venueStaff == null) {
            log.error("用户ID: {} 不是任何商家的员工", userId);
            throw new GloboxApplicationException("您不是商家员工，无权访问此资源");
        }

        log.debug("用户ID: {} 对应的商家ID: {}", userId, venueStaff.getMerchantId());
        return venueStaff.getMerchantId();
    }

}
