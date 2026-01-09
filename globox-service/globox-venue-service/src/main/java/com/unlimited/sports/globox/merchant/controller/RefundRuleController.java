package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.RefundRuleService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
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
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.*;

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
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 创建退款规则
     */
    @PostMapping
    public R<RefundRuleVo> createRefundRule(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Validated CreateRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 退款规则管理通常需要较高权限
        if (context.isStaff()) {
            merchantAuthUtil.validatePermission(context, PERMISSION_ORDER_MANAGE);
        }

        RefundRuleVo result = refundRuleService.createRefundRule(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 更新退款规则
     */
    @PutMapping
    public R<RefundRuleVo> updateRefundRule(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Validated UpdateRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        if (context.isStaff()) {
            merchantAuthUtil.validatePermission(context, PERMISSION_ORDER_MANAGE);
        }

        RefundRuleVo result = refundRuleService.updateRefundRule(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 删除退款规则
     */
    @DeleteMapping("/{ruleId}")
    public R<Long> deleteRefundRule(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long ruleId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        if (context.isStaff()) {
            merchantAuthUtil.validatePermission(context, PERMISSION_ORDER_MANAGE);
        }

        refundRuleService.deleteRefundRule(context.getMerchantId(), ruleId);
        return R.ok(ruleId);
    }

    /**
     * 获取退款规则详情
     */
    @GetMapping("/{ruleId}")
    public R<RefundRuleVo> getRefundRule(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long ruleId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        RefundRuleVo result = refundRuleService.getRefundRule(context.getMerchantId(), ruleId);
        return R.ok(result);
    }

    /**
     * 分页查询退款规则列表
     */
    @GetMapping
    public R<Page<RefundRuleSimpleVo>> queryRefundRules(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @Validated QueryRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        Page<RefundRuleSimpleVo> result = refundRuleService.queryRefundRules(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 绑定退款规则到场馆
     */
    @PostMapping("/bind")
    public R<Void> bindRefundRule(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @RequestBody @Validated BindRefundRuleDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        // 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());

        if (context.isStaff()) {
            merchantAuthUtil.validatePermission(context, PERMISSION_ORDER_MANAGE);
        }

        refundRuleService.bindRefundRule(context.getMerchantId(), dto);
        return R.ok();
    }

    /**
     * 设置默认退款规则
     */
    @PostMapping("/{ruleId}/set-default")
    public R<Void> setDefaultRule(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long ruleId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        if (context.isStaff()) {
            merchantAuthUtil.validatePermission(context, PERMISSION_ORDER_MANAGE);
        }

        refundRuleService.setDefaultRule(context.getMerchantId(), ruleId);
        return R.ok();
    }

    /**
     * 启用/禁用退款规则
     */
    @PostMapping("/{ruleId}/toggle-status")
    public R<Void> toggleRuleStatus(
            @RequestHeader(value = HEADER_EMPLOYEE_ID, required = false) Long employeeId,
            @RequestHeader(value = HEADER_MERCHANT_ROLE, required = false) String roleStr,
            @PathVariable Long ruleId,
            @RequestParam Boolean enabled) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId, roleStr);

        if (context.isStaff()) {
            merchantAuthUtil.validatePermission(context, PERMISSION_ORDER_MANAGE);
        }

        refundRuleService.toggleRuleStatus(context.getMerchantId(), ruleId, enabled);
        return R.ok();
    }

}