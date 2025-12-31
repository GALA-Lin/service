package com.unlimited.sports.globox.merchant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.model.merchant.dto.BindPriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.dto.CreatePriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.dto.QueryPriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.dto.UpdatePriceTemplateDto;
import com.unlimited.sports.globox.model.merchant.vo.PriceTemplateSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.PriceTemplateVo;

/**
 * @since 2025/12/24 10:56
 * 价格模板服务接口
 */

public interface PriceTemplateService {

    /**
     * 创建价格模板
     * @param merchantId 商家ID
     * @param dto 创建DTO
     * @return 创建的价格模板VO
     */
    PriceTemplateVo createPriceTemplate(Long merchantId, CreatePriceTemplateDto dto);

    /**
     * 更新价格模板
     * @param merchantId 商家ID
     * @param dto 更新DTO
     * @return 更新后的价格模板VO
     */
    PriceTemplateVo updatePriceTemplate(Long merchantId, UpdatePriceTemplateDto dto);

    /**
     * 删除价格模板
     * @param merchantId 商家ID
     * @param templateId 模板ID
     */
    void deletePriceTemplate(Long merchantId, Long templateId);

    /**
     * 获取价格模板详情
     * @param merchantId 商家ID
     * @param templateId 模板ID
     * @return 价格模板VO
     */
    PriceTemplateVo getPriceTemplate(Long merchantId, Long templateId);

    /**
     * 分页查询价格模板列表
     * @param merchantId 商家ID
     * @param dto 查询DTO
     * @return 价格模板分页列表
     */
    Page<PriceTemplateSimpleVo> queryPriceTemplates(Long merchantId, QueryPriceTemplateDto dto);

    /**
     * 绑定价格模板到场馆
     * @param merchantId 商家ID
     * @param dto 绑定DTO
     */
    void bindPriceTemplate(Long merchantId, BindPriceTemplateDto dto);

    /**
     * 设置默认价格模板
     * @param merchantId 商家ID
     * @param templateId 模板ID
     */
    void setDefaultTemplate(Long merchantId, Long templateId);
}