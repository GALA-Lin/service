package com.unlimited.sports.globox.dubbo.merchant;

import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * @since 2026/1/2 12:50
 * 退款规则查询 Dubbo 接口
 */
// TODO 商家模块处理退款规则，返回可以/不可以和原因
public interface MerchantRefundRuleDubboService {
    /**
     * 根据商家ID和场馆ID获取退款规则
     * 查询优先级：
     * 1. 场馆专属规则（venue_id匹配且is_default=1）
     * 2. 商家默认规则（venue_id为NULL且is_default=1）
     * 3. 如果都不存在，返回null
     *
     * @param request 包含merchantId和venueId的请求对象
     * @return 退款规则详情，不存在则返回null
     */
    MerchantRefundRuleResultDto getRefundRule(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRefundRuleQueryRequestDto request);

    /**
     * 根据规则ID直接获取退款规则（用于订单快照场景）
     *
     * @param ruleId 规则ID
     * @return 退款规则详情，不存在则返回null
     */
    MerchantRefundRuleResultDto getRefundRuleById(@NotNull(message = "规则ID不能为空") Long ruleId);

    RpcResult<MerchantRefundRuleJudgeResultVo> judgeApplicableRefundRule(
            @Valid @NotNull(message = "请求参数不能为空")
            MerchantRefundRuleJudgeRequestDto request);

}
