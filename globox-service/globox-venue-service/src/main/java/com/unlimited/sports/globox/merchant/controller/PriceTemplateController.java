package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.PriceTemplateService;
import com.unlimited.sports.globox.merchant.util.MerchantAuthContext;
import com.unlimited.sports.globox.merchant.util.MerchantAuthUtil;
import com.unlimited.sports.globox.model.merchant.dto.BindPriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.dto.CreatePriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.dto.QueryPriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.dto.UpdatePriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.entity.VenueStaff;
import com.unlimited.sports.globox.model.merchant.vo.PriceTemplateSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.PriceTemplateVo;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_MERCHANT_ACCOUNT_ID;
import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;
import static com.unlimited.sports.globox.merchant.util.MerchantConstants.*;

/**
 * @since 2025/12/27 10:21
 * 价格模板管理Controller
 */

@Slf4j
@RestController
@RequestMapping("/merchant/price-templates")
@RequiredArgsConstructor
public class PriceTemplateController {

    private final PriceTemplateService priceTemplateService;
    private final MerchantAuthUtil merchantAuthUtil;

    /**
     * 创建价格模板
     */
    @PostMapping
    public R<PriceTemplateVo> createPriceTemplate(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Validated CreatePriceTemplateDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        PriceTemplateVo result = priceTemplateService.createPriceTemplate(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 更新价格模板
     */
    @PutMapping
    public R<PriceTemplateVo> updatePriceTemplate(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Validated UpdatePriceTemplateDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);


        PriceTemplateVo result = priceTemplateService.updatePriceTemplate(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 删除价格模板
     */
    @DeleteMapping("/{templateId}")
    public R<Long> deletePriceTemplate(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long templateId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        priceTemplateService.deletePriceTemplate(context.getMerchantId(), templateId);
        return R.ok(templateId);
    }

    /**
     * 获取价格模板详情
     */
    @GetMapping("/{templateId}")
    public R<PriceTemplateVo> getPriceTemplate(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long templateId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        PriceTemplateVo result = priceTemplateService.getPriceTemplate(context.getMerchantId(), templateId);
        return R.ok(result);
    }

    /**
     * 分页查询价格模板列表
     */
    @GetMapping
    public R<Page<PriceTemplateSimpleVo>> queryPriceTemplates(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @Validated QueryPriceTemplateDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        Page<PriceTemplateSimpleVo> result = priceTemplateService.queryPriceTemplates(context.getMerchantId(), dto);
        return R.ok(result);
    }

    /**
     * 绑定价格模板到场馆
     */
    @PostMapping("/bind")
    public R<Void> bindPriceTemplate(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @RequestBody @Validated BindPriceTemplateDto dto) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        // 验证场馆访问权限
        merchantAuthUtil.validateVenueAccess(context, dto.getVenueId());

        priceTemplateService.bindPriceTemplate(context.getMerchantId(), dto);
        return R.ok();
    }

    /**
     * 设置默认价格模板
     */
    @PostMapping("/{templateId}/set-default")
    public R<Void> setDefaultTemplate(
            @RequestHeader(HEADER_MERCHANT_ACCOUNT_ID) Long employeeId,
            @RequestHeader(HEADER_MERCHANT_ID) Long merchantId,
            @RequestHeader(HEADER_MERCHANT_ROLE) String roleStr,
            @PathVariable Long templateId) {

        MerchantAuthContext context = merchantAuthUtil.validateAndGetContext(employeeId,merchantId, roleStr);

        priceTemplateService.setDefaultTemplate(context.getMerchantId(), templateId);
        return R.ok();
    }


}
