package com.unlimited.sports.globox.merchant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.model.merchant.dto.BindRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.dto.CreateRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.dto.QueryRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.dto.UpdateRefundRuleDto;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleVo;

/**
 * @since 2025/12/31 11:21
 * 退款规则服务接口
 */

public interface RefundRuleService {

    /**
     * 创建退款规则
     * @param merchantId 商家ID
     * @param dto 创建DTO
     * @return 创建的退款规则VO
     */
    RefundRuleVo createRefundRule(Long merchantId, CreateRefundRuleDto dto);

    /**
     * 更新退款规则
     * @param merchantId 商家ID
     * @param dto 更新DTO
     * @return 更新后的退款规则VO
     */
    RefundRuleVo updateRefundRule(Long merchantId, UpdateRefundRuleDto dto);

    /**
     * 删除退款规则
     * @param merchantId 商家ID
     * @param ruleId 规则ID
     */
    void deleteRefundRule(Long merchantId, Long ruleId);

    /**
     * 获取退款规则详情
     * @param merchantId 商家ID
     * @param ruleId 规则ID
     * @return 退款规则VO
     */
    RefundRuleVo getRefundRule(Long merchantId, Long ruleId);

    /**
     * 分页查询退款规则列表
     * @param merchantId 商家ID
     * @param dto 查询DTO
     * @return 退款规则分页列表
     */
    Page<RefundRuleSimpleVo> queryRefundRules(Long merchantId, QueryRefundRuleDto dto);

    /**
     * 绑定退款规则到场馆
     * @param merchantId 商家ID
     * @param dto 绑定DTO
     */
    void bindRefundRule(Long merchantId, BindRefundRuleDto dto);

    /**
     * 设置默认退款规则
     * @param merchantId 商家ID
     * @param ruleId 规则ID
     */
    void setDefaultRule(Long merchantId, Long ruleId);

    /**
     * 启用/禁用退款规则
     * @param merchantId 商家ID
     * @param ruleId 规则ID
     * @param enabled 是否启用
     */
    void toggleRuleStatus(Long merchantId, Long ruleId, Boolean enabled);
}
