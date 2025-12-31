package com.unlimited.sports.globox.merchant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.merchant.mapper.VenueStaffMapper;
import com.unlimited.sports.globox.merchant.service.PriceTemplateService;
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

import static com.unlimited.sports.globox.common.constants.RequestHeaderConstants.HEADER_USER_ID;
import static com.unlimited.sports.globox.common.result.UserAuthCode.TOKEN_EXPIRED;

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
    private final VenueStaffMapper venueStaffMapper;

    @PostMapping
    public R<PriceTemplateVo> createPriceTemplate(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestBody @Validated CreatePriceTemplateDto dto) {
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        PriceTemplateVo result = priceTemplateService.createPriceTemplate(merchantId, dto);
        return R.ok(result);
    }

    /**
     * 更新价格模板
     * @param userId 用户ID
     * @param dto 更新DTO
     * @return 更新后的价格模板
     */
    @PutMapping
    public R<PriceTemplateVo> updatePriceTemplate(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestBody @Validated UpdatePriceTemplateDto dto) {
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        PriceTemplateVo result = priceTemplateService.updatePriceTemplate(merchantId, dto);
        return R.ok(result);
    }

    /**
     * 删除价格模板
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 成功/失败
     */
    @DeleteMapping("/{templateId}")
    public R<Long> deletePriceTemplate(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable Long templateId) {
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        priceTemplateService.deletePriceTemplate(merchantId, templateId);
        return R.ok(templateId);
    }

    /**
     * 获取价格模板详情
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 价格模板详情
     */
    @GetMapping("/{templateId}")
    public R<PriceTemplateVo> getPriceTemplate(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable Long templateId) {
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        PriceTemplateVo result = priceTemplateService.getPriceTemplate(merchantId, templateId);
        return R.ok(result);
    }

    /**
     * 分页查询价格模板列表
     * @param userId 用户ID
     * @param dto 查询条件
     * @return 价格模板分页列表
     */
    @GetMapping
    public R<Page<PriceTemplateSimpleVo>> queryPriceTemplates(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @Validated QueryPriceTemplateDto dto) {
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        Page<PriceTemplateSimpleVo> result = priceTemplateService.queryPriceTemplates(merchantId, dto);
        return R.ok(result);
    }

    /**
     * 绑定价格模板到场馆
     * @param userId 用户ID
     * @param dto 绑定DTO
     * @return 成功/失败
     */
    @PostMapping("/bind")
    public R<Void> bindPriceTemplate(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @RequestBody @Validated BindPriceTemplateDto dto) {
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        priceTemplateService.bindPriceTemplate(merchantId, dto);
        return R.ok();
    }

    /**
     * 设置默认价格模板
     * @param userId 用户ID
     * @param templateId 模板ID
     * @return 成功/失败
     */
    @PostMapping("/{templateId}/set-default")
    public R<Void> setDefaultTemplate(
            @RequestHeader(value = HEADER_USER_ID , required = false) Long userId,
            @PathVariable Long templateId) {
        if (userId == null) {
            log.error("请求头中缺少User-Id");
            throw new GloboxApplicationException(TOKEN_EXPIRED.getCode(), TOKEN_EXPIRED.getMessage());
        }

        Long merchantId = getMerchantIdByUserId(userId);
        priceTemplateService.setDefaultTemplate(merchantId, templateId);
        return R.ok();
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
