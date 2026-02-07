package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.service.RefundRuleService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ID;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.HEADER_MERCHANT_ROLE;

/**
 * 退款规则管理Controller
 *
 * @since 2025/12/31
 */
@Slf4j
@RestController
@RequestMapping("/merchant/refund-rules")
@RequiredArgsConstructor
public class RefundRuleController {

    private final RefundRuleService refundRuleService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 创建退款规则
     */
    @PostMapping
    public R<RefundRuleVo> createRefundRule(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Validated CreateRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        // 如果指定了场馆，验证场馆访问权限
        if (dto.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());
        }

        RefundRuleVo result = refundRuleService.createRefundRule(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 更新退款规则
     */
    @PutMapping
    public R<RefundRuleVo> updateRefundRule(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Validated UpdateRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        RefundRuleVo result = refundRuleService.updateRefundRule(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 删除退款规则
     */
    @DeleteMapping("/{ruleId}")
    public R<Void> deleteRefundRule(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long ruleId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        refundRuleService.deleteRefundRule(context.getMerchantId(), ruleId);
        return R.ok();
    }

    /**
     * 获取退款规则详情
     */
    @GetMapping("/{ruleId}")
    public R<RefundRuleVo> getRefundRule(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long ruleId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        RefundRuleVo result = refundRuleService.getRefundRule(context.getMerchantId(), ruleId);
        return R.ok(result);
    }

    /**
     * 分页查询退款规则列表
     */
    @GetMapping
    public R<Page<RefundRuleVo>> queryRefundRules(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Validated QueryRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        // 如果指定了场馆，验证场馆访问权限
        if (dto.getVenueId() != null) {
            merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());
        }

        Page<RefundRuleVo> result = refundRuleService.queryRefundRules(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 绑定普通退款规则到场馆
     */
    @PostMapping("/bind-normal")
    public R<Void> bindNormalRefundRule(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Validated BindRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        // 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());

        refundRuleService.bindRefundRuleToVenue(context.getMerchantId(), dto, false);
        return R.ok();
    }

    /**
     * 绑定活动退款规则到场馆
     */
    @PostMapping("/bind-activity")
    public R<Void> bindActivityRefundRule(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Validated BindRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        // 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());

        refundRuleService.bindRefundRuleToVenue(context.getMerchantId(), dto, true);
        return R.ok();
    }

    /**
     * 设置默认退款规则
     */
    @PostMapping("/{ruleId}/set-default")
    public R<Void> setDefaultRule(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long ruleId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, merchantId, roleStr);

        refundRuleService.setDefaultRule(context.getMerchantId(), ruleId);
        return R.ok();
    }
}