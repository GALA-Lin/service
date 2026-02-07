package com.unlimited.sports.globox.merchant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.model.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleVo;

/**
 * 退款规则服务接口
 *
 * @since 2025/12/31
 */
public interface RefundRuleService {

    /**
     * 创建退款规则
     *
     * @param merchantId 商家ID
     * @param dto 创建DTO
     * @return 退款规则详情
     */
    RefundRuleVo createRefundRule(Long merchantId, CreateRefundRuleDto dto);

    /**
     * 更新退款规则
     *
     * @param merchantId 商家ID
     * @param dto 更新DTO
     * @return 退款规则详情
     */
    RefundRuleVo updateRefundRule(Long merchantId, UpdateRefundRuleDto dto);

    /**
     * 删除退款规则
     *
     * @param merchantId 商家ID
     * @param ruleId 规则ID
     */
    void deleteRefundRule(Long merchantId, Long ruleId);

    /**
     * 获取退款规则详情
     *
     * @param merchantId 商家ID
     * @param ruleId 规则ID
     * @return 退款规则详情
     */
    RefundRuleVo getRefundRule(Long merchantId, Long ruleId);

    /**
     * 分页查询退款规则列表
     *
     * @param merchantId 商家ID
     * @param dto 查询条件
     * @return 退款规则分页列表
     */
    Page<RefundRuleVo> queryRefundRules(Long merchantId, QueryRefundRuleDto dto);

    /**
     * 绑定退款规则到场馆
     *
     * @param merchantId 商家ID
     * @param dto 绑定DTO
     * @param isActivityRule 是否为活动退款规则（true=活动，false=普通）
     */
    void bindRefundRuleToVenue(Long merchantId, BindRefundRuleDto dto, boolean isActivityRule);

    /**
     * 设置为默认规则
     *
     * @param merchantId 商家ID
     * @param ruleId 规则ID
     */
    void setDefaultRule(Long merchantId, Long ruleId);
}