package com.unlimited.sports.globox.merchant.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unlimited.sports.globox.dubbo.merchant.MerchantRefundRuleDubboService;
import com.unlimited.sports.globox.dubbo.merchant.dto.*;
import com.unlimited.sports.globox.merchant.mapper.VenueRefundRuleDetailMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueRefundRuleMapper;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRule;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRuleDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 2026/1/2 12:57
 * 退款规则查询 Dubbo 接口实现
 */
@Slf4j
@Component
@DubboService(group = "rpc")
@RequiredArgsConstructor
public class MerchantRefundRuleDubboServiceImpl implements MerchantRefundRuleDubboService {

    private final VenueRefundRuleMapper refundRuleMapper;
    private final VenueRefundRuleDetailMapper refundRuleDetailMapper;

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
    @Override
    public MerchantRefundRuleResultDto getRefundRule(MerchantRefundRuleQueryRequestDto request) {
        log.info("查询退款规则 - merchantId: {}, venueId: {}",
                request.getMerchantId(), request.getVenueId());

        // 1. 优先查询场馆专属默认规则
        VenueRefundRule rule = refundRuleMapper.selectDefaultRule(
                request.getMerchantId(),
                request.getVenueId()
        );

        // 2. 如果没有场馆专属规则，查询商家默认规则
        if (rule == null) {
            log.debug("未找到场馆专属规则，查询商家默认规则");
            rule = refundRuleMapper.selectDefaultRule(
                    request.getMerchantId(),
                    null
            );
        }

        // 3. 如果都没有，返回null
        if (rule == null) {
            log.warn("未找到退款规则 - merchantId: {}, venueId: {}",
                    request.getMerchantId(), request.getVenueId());
            return null;
        }

        return convertToDto(rule);
    }

    /**
     * 根据规则ID直接获取退款规则（用于订单快照场景）
     *
     * @param ruleId 规则ID
     * @return 退款规则详情，不存在则返回null
     */
    @Override
    public MerchantRefundRuleResultDto getRefundRuleById(Long ruleId) {
        log.info("根据ID查询退款规则 - ruleId: {}", ruleId);

        VenueRefundRule rule = refundRuleMapper.selectOne(
                new LambdaQueryWrapper<VenueRefundRule>()
                        .eq(VenueRefundRule::getVenueRefundRuleId, ruleId)
                        .eq(VenueRefundRule::getIsEnabled, 1)
        );

        if (rule == null) {
            log.warn("退款规则不存在或已禁用 - ruleId: {}", ruleId);
            return null;
        }

        return convertToDto(rule);
    }

    /**
     * 判断是否适用退款规则
     * @param request
     * @return
     */
    @Override
    public MerchantRefundRuleJudgeResultVo judgeApplicableRefundRule(MerchantRefundRuleJudgeRequestDto request) {
        log.info("判断是否适用退款规则, venueId: {}, 活动开始时间: {}, 退款申请时间: {}",
                 request.getVenueId(), request.getEventStartTime(), request.getRefundApplyTime());

        // 1. 核心时间合法性校验
        LocalDateTime eventStartTime = request.getEventStartTime();
        LocalDateTime refundApplyTime = request.getRefundApplyTime();

        // 退款申请时间在活动开始之后，直接不允许退款
        if (refundApplyTime.isAfter(eventStartTime)) {
            String reason = "退款申请时间晚于活动开始时间，不支持退款";
            log.warn(reason);
            return MerchantRefundRuleJudgeResultVo.builder()
                    .canRefund(false)
                    .refundPercentage(BigDecimal.ZERO)
                    .reason(reason)
                    .build();
        }

        // 计算：退款申请时间 距离 活动开始时间 的提前小时数
        Duration duration = Duration.between(refundApplyTime, eventStartTime);
        long hoursBeforeEvent = duration.toHours(); // 提前的小时数（比如提前48小时、24小时）
        log.debug("退款申请时间距离活动开始时间提前 {} 小时", hoursBeforeEvent);

        // 2. 查询有效的退款规则
        MerchantRefundRuleQueryRequestDto queryRequest = MerchantRefundRuleQueryRequestDto.builder()
//                .merchantId(request.getMerchantId())
                .venueId(request.getVenueId())
                .build();
        MerchantRefundRuleResultDto refundRule = getRefundRule(queryRequest);

        if (refundRule == null || !refundRule.getIsEnabled()) {
            String reason = "未找到可用的退款规则，不支持退款";
            log.warn(reason);
            return MerchantRefundRuleJudgeResultVo.builder()
                    .canRefund(false)
                    .refundPercentage(BigDecimal.ZERO)
                    .reason(reason)
                    .build();
        }

        // 3. 检查规则明细
        List<MerchantRefundRuleDetailDto> ruleDetails = refundRule.getDetails();
        if (ruleDetails == null || ruleDetails.isEmpty()) {
            String reason = "退款规则未配置明细，不支持退款";
            log.warn(reason);
            return MerchantRefundRuleJudgeResultVo.builder()
                    .canRefund(false)
                    .refundPercentage(BigDecimal.ZERO)
                    .reason(reason)
                    .build();
        }

        // 按优先级排序
        ruleDetails.sort(Comparator.comparingInt(MerchantRefundRuleDetailDto::getSortOrderNum));

        for (MerchantRefundRuleDetailDto detail : ruleDetails) {
            Integer minHours = detail.getMinHoursBefore(); // 规则要求的最小提前小时数
            Integer maxHours = detail.getMaxHoursBefore(); // 规则要求的最大提前小时数
            BigDecimal refundPercentage = detail.getRefundPercentage();

            // 判断：退款申请的提前小时数 是否匹配规则的时间范围（核心逻辑）
            boolean isInTimeRange;
            if (maxHours == null) {
                // 无上限（比如规则10：min=48，max=null → 提前≥48小时）
                isInTimeRange = hoursBeforeEvent >= minHours;
            } else {
                // 有上下限（比如规则11：min=0，max=48 → 提前0~48小时）
                isInTimeRange = hoursBeforeEvent >= minHours && hoursBeforeEvent <= maxHours;
            }

            if (isInTimeRange) {
                boolean canRefund = refundPercentage.compareTo(BigDecimal.ZERO) > 0;
                String reason = canRefund ? null : detail.getRefundRuleDetailDesc();

                log.info("匹配到退款规则明细 [{}]，提前小时数: {}, 是否可退: {}, 退款比例: {}%",
                        detail.getVenueRefundRuleDetailId(), hoursBeforeEvent, canRefund, refundPercentage);

                return MerchantRefundRuleJudgeResultVo.builder()
                        .canRefund(canRefund)
                        .refundPercentage(refundPercentage)
                        .reason(reason)
                        .build();
            }
        }

        // 4. 未匹配任何规则明细
        String reason = "未匹配到适用的退款规则，不支持退款";
        log.warn(reason);
        return MerchantRefundRuleJudgeResultVo.builder()
                .canRefund(false)
                .refundPercentage(BigDecimal.ZERO)
                .reason(reason)
                .build();
    }

    /**
     * 转换为DTO
     */
    private MerchantRefundRuleResultDto convertToDto(VenueRefundRule rule) {
        // 查询规则明细
        List<VenueRefundRuleDetail> details = refundRuleDetailMapper
                .selectByRuleId(rule.getVenueRefundRuleId());

        // 转换明细
        List<MerchantRefundRuleDetailDto> detailDtos = details.stream()
                .map(this::convertDetailToDto)
                .collect(Collectors.toList());

        return MerchantRefundRuleResultDto.builder()
                .venueRefundRuleId(rule.getVenueRefundRuleId())
                .merchantId(rule.getMerchantId())
                .venueId(rule.getVenueId())
                .venueRefundRuleName(rule.getVenueRefundRuleName())
                .isDefault(rule.getIsDefault() == 1)
                .isEnabled(rule.getIsEnabled() == 1)
                .venueRefundRuleDesc(rule.getVenueRefundRuleDesc())
                .details(detailDtos)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    /**
     * 转换明细为DTO
     */
    private MerchantRefundRuleDetailDto convertDetailToDto(
            VenueRefundRuleDetail detail) {

        String timeRangeDesc = buildTimeRangeDesc(
                detail.getMinHoursBefore(),
                detail.getMaxHoursBefore()
        );

        return MerchantRefundRuleDetailDto.builder()
                .venueRefundRuleDetailId(detail.getVenueRefundRuleDetailId())
                .minHoursBefore(detail.getMinHoursBefore())
                .maxHoursBefore(detail.getMaxHoursBefore())
                .refundPercentage(detail.getRefundPercentage())
                .handlingFeePercentage(detail.getHandlingFeePercentage())
                .refundRuleDetailDesc(detail.getRefundRuleDetailDesc())
                .sortOrderNum(detail.getSortOrderNum())
                .timeRangeDesc(timeRangeDesc)
                .build();
    }

    /**
     * 构建时间范围描述
     */
    private String buildTimeRangeDesc(Integer minHours, Integer maxHours) {
        if (maxHours == null) {
            return String.format("%d小时以上", minHours);
        } else {
            return String.format("%d-%d小时", minHours, maxHours);
        }
    }
}
