package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.VenueMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueRefundRuleDetailMapper;
import com.unlimited.sports.globox.merchant.mapper.VenueRefundRuleMapper;
import com.unlimited.sports.globox.merchant.service.RefundRuleService;
import com.unlimited.sports.globox.model.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRule;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRuleDetail;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleDetailVo;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleSimpleVo;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import static com.unlimited.sports.globox.model.merchant.enums.DefaultStatusEnum.*;
import static com.unlimited.sports.globox.model.merchant.enums.EnableStatusEnum.*;

/**
 * @since 2025/12/31 11:22
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundRuleServiceImpl implements RefundRuleService {

    private final VenueRefundRuleMapper refundRuleMapper;
    private final VenueRefundRuleDetailMapper refundRuleDetailMapper;
    private final VenueMapper venueMapper;

    /**
     * 创建退款规则
     *
     * @param merchantId 商家ID
     * @param dto        创建DTO
     * @return 创建的退款规则VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundRuleVo createRefundRule(Long merchantId, CreateRefundRuleDto dto) {
        // 验证明细时间阶梯
        validateRuleDetails(dto.getDetails());

        // 如果指定了场馆，验证场馆归属
        if (dto.getVenueId() != null) {
            validateVenueOwnership(merchantId, dto.getVenueId());
        }

        // 如果设置为默认规则，先清除其他默认规则
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultRules(merchantId, dto.getVenueId());
        }

        // 创建退款规则
        VenueRefundRule rule = VenueRefundRule.builder()
                .merchantId(merchantId)
                .venueId(dto.getVenueId())
                .venueRefundRuleName(dto.getVenueRefundRuleName())
                .isDefault(DefaultStatusfromBoolean(dto.getIsDefault()))
                .isEnabled(ENABLED)
                .venueRefundRuleDesc(dto.getVenueRefundRuleDesc())
                .build();

        refundRuleMapper.insert(rule);

        // 创建退款规则明细
        List<VenueRefundRuleDetail> details = dto.getDetails().stream()
                .map(detailDto -> buildRuleDetail(rule.getVenueRefundRuleId(), detailDto))
                .collect(Collectors.toList());

        if (!details.isEmpty()) {
            refundRuleDetailMapper.batchInsert(details);
        }

        log.info("创建退款规则成功 - merchantId: {}, ruleId: {}, ruleName: {}",
                merchantId, rule.getVenueRefundRuleId(), dto.getVenueRefundRuleName());

        return getRefundRule(merchantId, rule.getVenueRefundRuleId());
    }

    /**
     * 更新退款规则
     *
     * @param merchantId 商家ID
     * @param dto        更新DTO
     * @return 更新后的退款规则VO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundRuleVo updateRefundRule(Long merchantId, UpdateRefundRuleDto dto) {
        // 验证规则归属
        VenueRefundRule rule = refundRuleMapper.selectById(dto.getVenueRefundRuleId());
        if (rule == null) {
            throw new GloboxApplicationException("退款规则不存在");
        }
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该退款规则");
        }

        // 更新规则基本信息
        if (StringUtils.hasText(dto.getVenueRefundRuleName())) {
            rule.setVenueRefundRuleName(dto.getVenueRefundRuleName());
        }
        if (dto.getIsEnabled() != null) {
            rule.setIsEnabled(EnableStatusfromBoolean(dto.getIsEnabled()));
        }
        if (dto.getVenueRefundRuleDesc() != null) {
            rule.setVenueRefundRuleDesc(dto.getVenueRefundRuleDesc());
        }

        // 如果设置为默认规则，清除其他默认规则
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultRules(merchantId, rule.getVenueId());
            rule.setIsDefault(DEFAULT);
        } else if (Boolean.FALSE.equals(dto.getIsDefault())) {
            rule.setIsDefault(NOT_DEFAULT);
        }

        refundRuleMapper.updateById(rule);

        // 如果提供了新的明细列表，更新明细
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            validateRuleDetails(dto.getDetails());

            // 删除原有明细
            refundRuleDetailMapper.deleteByRuleId(dto.getVenueRefundRuleId());

            // 插入新明细
            List<VenueRefundRuleDetail> details = dto.getDetails().stream()
                    .map(detailDto -> buildRuleDetail(dto.getVenueRefundRuleId(), detailDto))
                    .collect(Collectors.toList());

            if (!details.isEmpty()) {
                refundRuleDetailMapper.batchInsert(details);
            }
        }

        log.info("更新退款规则成功 - merchantId: {}, ruleId: {}", merchantId, dto.getVenueRefundRuleId());

        return getRefundRule(merchantId, dto.getVenueRefundRuleId());
    }

    /**
     * 删除退款规则
     *
     * @param merchantId 商家ID
     * @param ruleId     规则ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRefundRule(Long merchantId, Long ruleId) {
        // 验证规则归属
        VenueRefundRule rule = refundRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new GloboxApplicationException("退款规则不存在");
        }
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该退款规则");
        }

        // 检查是否有场馆正在使用该规则
        Integer venueCount = refundRuleMapper.countVenuesByRuleId(ruleId);
        if (venueCount > 0) {
            throw new GloboxApplicationException(
                    String.format("该退款规则正在被 %d 个场馆使用，无法删除", venueCount));
        }

        // 删除明细
        refundRuleDetailMapper.deleteByRuleId(ruleId);

        // 删除规则（逻辑删除）
        rule.setIsEnabled(DISABLED);
        refundRuleMapper.updateById(rule);

        log.info("删除退款规则成功 - merchantId: {}, ruleId: {}", merchantId, ruleId);
    }

    /**
     * 获取退款规则详情
     *
     * @param merchantId 商家ID
     * @param ruleId     规则ID
     * @return 退款规则VO
     */
    @Override
    public RefundRuleVo getRefundRule(Long merchantId, Long ruleId) {
        // 验证规则归属
        VenueRefundRule rule = refundRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new GloboxApplicationException("退款规则不存在");
        }
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权访问该退款规则");
        }

        // 查询明细
        List<VenueRefundRuleDetail> details = refundRuleDetailMapper.selectByRuleId(ruleId);

        // 查询使用该规则的场馆数量
        Integer venueCount = refundRuleMapper.countVenuesByRuleId(ruleId);

        // 查询场馆名称
        String venueName = null;
        if (rule.getVenueId() != null) {
            Venue venue = venueMapper.selectById(rule.getVenueId());
            if (venue != null) {
                venueName = venue.getName();
            }
        }

        return convertToVo(rule, details, venueCount, venueName);
    }
    /**
     * 分页查询退款规则列表
     *
     * @param merchantId 商家ID
     * @param dto        查询DTO
     * @return 退款规则分页列表
     */
    @Override
    public Page<RefundRuleSimpleVo> queryRefundRules(Long merchantId, QueryRefundRuleDto dto) {
        Page<VenueRefundRule> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<VenueRefundRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VenueRefundRule::getMerchantId, merchantId);

        if (dto.getVenueId() != null) {
            wrapper.eq(VenueRefundRule::getVenueId, dto.getVenueId());
        }
        if (dto.getIsDefault() != null) {
            wrapper.eq(VenueRefundRule::getIsDefault, dto.getIsDefault() ? 1 : 0);
        }
        if (dto.getIsEnabled() != null) {
            wrapper.eq(VenueRefundRule::getIsEnabled, dto.getIsEnabled() ? 1 : 0);
        }
        if (StringUtils.hasText(dto.getVenueRefundRuleName())) {
            wrapper.like(VenueRefundRule::getVenueRefundRuleName, dto.getVenueRefundRuleName());
        }

        wrapper.orderByDesc(VenueRefundRule::getIsDefault)
                .orderByDesc(VenueRefundRule::getCreatedAt);

        Page<VenueRefundRule> rulePage = refundRuleMapper.selectPage(page, wrapper);

        // 转换为简要VO
        Page<RefundRuleSimpleVo> resultPage = new Page<>(dto.getPageNum(), dto.getPageSize());
        resultPage.setTotal(rulePage.getTotal());
        resultPage.setRecords(rulePage.getRecords().stream()
                .map(this::convertToSimpleVo)
                .collect(Collectors.toList()));

        return resultPage;
    }
    /**
     * 绑定退款规则到场馆
     *
     * @param merchantId 商家ID
     * @param dto        绑定DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindRefundRule(Long merchantId, BindRefundRuleDto dto) {
        // 验证场馆归属
        validateVenueOwnership(merchantId, dto.getVenueId());

        // 验证规则归属
        VenueRefundRule rule = refundRuleMapper.selectById(dto.getVenueRefundRuleId());
        if (rule == null) {
            throw new GloboxApplicationException("退款规则不存在");
        }
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权使用该退款规则");
        }

        // 更新场馆的退款规则
        Venue venue = venueMapper.selectById(dto.getVenueId());
        venue.setVenueRefundRuleId(dto.getVenueRefundRuleId());
        venueMapper.updateById(venue);

        log.info("绑定退款规则成功 - venueId: {}, ruleId: {}", dto.getVenueId(), dto.getVenueRefundRuleId());
    }

    /**
     * 设置默认退款规则
     *
     * @param merchantId 商家ID
     * @param ruleId     规则ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultRule(Long merchantId, Long ruleId) {
        // 验证规则归属
        VenueRefundRule rule = refundRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new GloboxApplicationException("退款规则不存在");
        }
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该退款规则");
        }

        // 清除其他默认规则
        clearDefaultRules(merchantId, rule.getVenueId());

        // 设置为默认规则
        rule.setIsDefault(DEFAULT);
        refundRuleMapper.updateById(rule);

        log.info("设置默认退款规则成功 - merchantId: {}, ruleId: {}", merchantId, ruleId);
    }

    /**
     * 启用/禁用退款规则
     *
     * @param merchantId 商家ID
     * @param ruleId     规则ID
     * @param enabled    是否启用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleRuleStatus(Long merchantId, Long ruleId, Boolean enabled) {
        // 验证规则归属
        VenueRefundRule rule = refundRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new GloboxApplicationException("退款规则不存在");
        }
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该退款规则");
        }

        rule.setIsEnabled(EnableStatusfromBoolean(enabled));
        refundRuleMapper.updateById(rule);

        log.info("切换退款规则状态成功 - merchantId: {}, ruleId: {}, enabled: {}",
                merchantId, ruleId, enabled);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 验证明细时间阶梯是否合法
     */
    private void validateRuleDetails(List<RefundRuleDetailDto> details) {
        if (details == null || details.isEmpty()) {
            throw new GloboxApplicationException("退款规则明细不能为空");
        }

        // 检查时间阶梯是否有重叠
        for (int i = 0; i < details.size(); i++) {
            RefundRuleDetailDto detail1 = details.get(i);

            // 验证单个明细的有效性
            if (detail1.getMaxHoursBefore() != null &&
                    detail1.getMinHoursBefore() >= detail1.getMaxHoursBefore()) {
                throw new GloboxApplicationException(
                        "时间阶梯无效：最小提前小时数必须小于最大提前小时数");
            }

            // 检查与其他明细是否重叠
            for (int j = i + 1; j < details.size(); j++) {
                RefundRuleDetailDto detail2 = details.get(j);

                boolean overlap = checkTimeOverlap(
                        detail1.getMinHoursBefore(), detail1.getMaxHoursBefore(),
                        detail2.getMinHoursBefore(), detail2.getMaxHoursBefore()
                );

                if (overlap) {
                    throw new GloboxApplicationException("时间阶梯存在重叠");
                }
            }
        }
    }

    /**
     * 检查两个时间阶梯是否重叠
     */
    private boolean checkTimeOverlap(Integer min1, Integer max1, Integer min2, Integer max2) {
        // 转换为实际范围进行比较
        int actualMax1 = (max1 != null) ? max1 : Integer.MAX_VALUE;
        int actualMax2 = (max2 != null) ? max2 : Integer.MAX_VALUE;

        // 检查是否有重叠：[min1, actualMax1) 和 [min2, actualMax2)
        return !(actualMax1 <= min2 || actualMax2 <= min1);
    }

    /**
     * 验证场馆归属
     */
    private void validateVenueOwnership(Long merchantId, Long venueId) {
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null || !venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场馆");
        }
    }

    /**
     * 清除其他默认规则
     */
    private void clearDefaultRules(Long merchantId, Long venueId) {
        LambdaQueryWrapper<VenueRefundRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VenueRefundRule::getMerchantId, merchantId)
                .eq(VenueRefundRule::getIsDefault, 1);

        if (venueId != null) {
            wrapper.eq(VenueRefundRule::getVenueId, venueId);
        } else {
            wrapper.isNull(VenueRefundRule::getVenueId);
        }

        List<VenueRefundRule> rules = refundRuleMapper.selectList(wrapper);
        for (VenueRefundRule rule : rules) {
            rule.setIsDefault(NOT_DEFAULT);
            refundRuleMapper.updateById(rule);
        }
    }

    /**
     * 构建规则明细实体
     */
    private VenueRefundRuleDetail buildRuleDetail(Long ruleId, RefundRuleDetailDto dto) {
        return VenueRefundRuleDetail.builder()
                .venueRefundRuleId(ruleId)
                .minHoursBefore(dto.getMinHoursBefore())
                .maxHoursBefore(dto.getMaxHoursBefore())
                .refundPercentage(dto.getRefundPercentage())
                .needContactMerchant(dto.getNeedContactMerchant() ? 1 : 0)
                .handlingFeePercentage(dto.getHandlingFeePercentage())
                .refundRuleDetailDesc(dto.getRefundRuleDetailDesc())
                .sortOrderNum(dto.getSortOrderNum())
                .build();
    }

    /**
     * 转换为详情VO
     */
    private RefundRuleVo convertToVo(VenueRefundRule rule,
                                     List<VenueRefundRuleDetail> details,
                                     Integer venueCount,
                                     String venueName) {
        List<RefundRuleDetailVo> detailVos = details.stream()
                .map(this::convertDetailToVo)
                .collect(Collectors.toList());

        return RefundRuleVo.builder()
                .venueRefundRuleId(rule.getVenueRefundRuleId())
                .merchantId(rule.getMerchantId())
                .venueId(rule.getVenueId())
                .venueName(venueName)
                .venueRefundRuleName(rule.getVenueRefundRuleName())
                .isDefault(DEFAULT.equals(rule.getIsDefault()))
                .isEnabled(ENABLED.equals(rule.getIsEnabled()))
                .venueRefundRuleDesc(rule.getVenueRefundRuleDesc())
                .details(detailVos)
                .venueCount(venueCount)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    /**
     * 转换明细为VO
     */
    private RefundRuleDetailVo convertDetailToVo(VenueRefundRuleDetail detail) {
        String timeRangeDesc = buildTimeRangeDesc(
                detail.getMinHoursBefore(),
                detail.getMaxHoursBefore()
        );

        return RefundRuleDetailVo.builder()
                .venueRefundRuleDetailId(detail.getVenueRefundRuleDetailId())
                .minHoursBefore(detail.getMinHoursBefore())
                .maxHoursBefore(detail.getMaxHoursBefore())
                .refundPercentage(detail.getRefundPercentage())
                .needContactMerchant(detail.getNeedContactMerchant() == 1)
                .handlingFeePercentage(detail.getHandlingFeePercentage())
                .refundRuleDetailDesc(detail.getRefundRuleDetailDesc())
                .sortOrderNum(detail.getSortOrderNum())
                .timeRangeDesc(timeRangeDesc)
                .createdAt(detail.getCreatedAt())
                .updatedAt(detail.getUpdatedAt())
                .build();
    }

    /**
     * 转换为简要VO
     */
    private RefundRuleSimpleVo convertToSimpleVo(VenueRefundRule rule) {
        // 查询明细
        List<VenueRefundRuleDetail> details = refundRuleDetailMapper.selectByRuleId(
                rule.getVenueRefundRuleId());

        // 生成规则摘要
        String ruleSummary = buildRuleSummary(details);

        // 查询场馆名称
        String venueName = null;
        if (rule.getVenueId() != null) {
            Venue venue = venueMapper.selectById(rule.getVenueId());
            if (venue != null) {
                venueName = venue.getName();
            }
        }

        // 查询使用该规则的场馆数量
        Integer venueCount = refundRuleMapper.countVenuesByRuleId(rule.getVenueRefundRuleId());

        return RefundRuleSimpleVo.builder()
                .venueRefundRuleId(rule.getVenueRefundRuleId())
                .venueRefundRuleName(rule.getVenueRefundRuleName())
                .venueId(rule.getVenueId())
                .venueName(venueName)
                .isDefault(DEFAULT.equals(rule.getIsDefault()))
                .isEnabled(ENABLED.equals(rule.getIsEnabled()))
                .venueRefundRuleDesc(rule.getVenueRefundRuleDesc())
                .ruleSummary(ruleSummary)
                .detailCount(details.size())
                .venueCount(venueCount)
                .createdAt(rule.getCreatedAt())
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

    /**
     * 构建规则摘要
     */
    private String buildRuleSummary(List<VenueRefundRuleDetail> details) {
        if (details.isEmpty()) {
            return "无规则";
        }

        return details.stream()
                .limit(3)  // 只显示前3条
                .map(detail -> {
                    String timeDesc = buildTimeRangeDesc(
                            detail.getMinHoursBefore(),
                            detail.getMaxHoursBefore()
                    );
                    return String.format("%s退%.0f%%", timeDesc, detail.getRefundPercentage());
                })
                .collect(Collectors.joining("，"));
    }
}
