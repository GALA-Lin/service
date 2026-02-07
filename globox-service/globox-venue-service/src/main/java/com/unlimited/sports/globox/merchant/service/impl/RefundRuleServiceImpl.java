package com.unlimited.sports.globox.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.merchant.mapper.*;
import com.unlimited.sports.globox.merchant.service.RefundRuleService;
import com.unlimited.sports.globox.model.merchant.dto.*;
import com.unlimited.sports.globox.model.merchant.entity.Venue;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRule;
import com.unlimited.sports.globox.model.merchant.entity.VenueRefundRuleDetail;
import com.unlimited.sports.globox.model.merchant.enums.DefaultStatusEnum;
import com.unlimited.sports.globox.model.merchant.enums.EnableStatusEnum;
import com.unlimited.sports.globox.model.merchant.vo.RefundRuleVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 退款规则服务实现
 *
 * @since 2025/12/31
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundRuleServiceImpl implements RefundRuleService {

    private final VenueRefundRuleMapper refundRuleMapper;
    private final VenueRefundRuleDetailMapper refundRuleDetailMapper;
    private final VenueMapper venueMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundRuleVo createRefundRule(Long merchantId, CreateRefundRuleDto dto) {
        log.info("创建退款规则 - merchantId: {}, ruleName: {}", merchantId, dto.getVenueRefundRuleName());

        // 1. 验证场馆归属（如果指定了场馆）
        if (dto.getVenueId() != null) {
            validateVenueBelongsToMerchant(merchantId, dto.getVenueId());
        }

        // 2. 验证规则明细
        validateRuleDetails(dto.getDetails());

        // 3. 如果设置为默认规则，先取消其他默认规则
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultRules(merchantId, dto.getVenueId());
        }

        // 4. 创建退款规则
        VenueRefundRule rule = VenueRefundRule.builder()
                .merchantId(merchantId)
                .venueId(dto.getVenueId())
                .venueRefundRuleName(dto.getVenueRefundRuleName())
                .isDefault(DefaultStatusEnum.DefaultStatusfromBoolean(dto.getIsDefault()).getCode())
                .isEnabled(EnableStatusEnum.ENABLED.getCode())
                .venueRefundRuleDesc(dto.getVenueRefundRuleDesc())
                .build();

        refundRuleMapper.insert(rule);
        log.info("退款规则创建成功 - ruleId: {}", rule.getVenueRefundRuleId());

        // 5. 创建退款规则明细
        List<VenueRefundRuleDetail> details = new ArrayList<>();
        for (int i = 0; i < dto.getDetails().size(); i++) {
            RefundRuleDetailDto detailDto = dto.getDetails().get(i);

            VenueRefundRuleDetail detail = VenueRefundRuleDetail.builder()
                    .venueRefundRuleId(rule.getVenueRefundRuleId())
                    .minHoursBefore(detailDto.getMinHoursBefore())
                    .maxHoursBefore(detailDto.getMaxHoursBefore())
                    .refundPercentage(detailDto.getRefundPercentage())
                    .handlingFeePercentage(detailDto.getHandlingFeePercentage())
                    .refundRuleDetailDesc(detailDto.getRefundRuleDetailDesc())
                    .sortOrderNum(detailDto.getSortOrderNum() != null ? detailDto.getSortOrderNum() : i)
                    .build();

            details.add(detail);
        }

        refundRuleDetailMapper.batchInsert(details);
        log.info("退款规则明细创建成功 - ruleId: {}, detailCount: {}",
                rule.getVenueRefundRuleId(), details.size());

        return getRefundRule(merchantId, rule.getVenueRefundRuleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundRuleVo updateRefundRule(Long merchantId, UpdateRefundRuleDto dto) {
        log.info("更新退款规则 - merchantId: {}, ruleId: {}", merchantId, dto.getVenueRefundRuleId());

        // 1. 验证规则归属
        VenueRefundRule rule = validateRuleBelongsToMerchant(merchantId, dto.getVenueRefundRuleId());

        // 2. 更新基本信息
        boolean needUpdate = false;

        if (StringUtils.hasText(dto.getVenueRefundRuleName())) {
            rule.setVenueRefundRuleName(dto.getVenueRefundRuleName());
            needUpdate = true;
        }

        if (dto.getIsDefault() != null) {
            if (Boolean.TRUE.equals(dto.getIsDefault())) {
                clearDefaultRules(merchantId, rule.getVenueId());
            }
            rule.setIsDefault(DefaultStatusEnum.DefaultStatusfromBoolean(dto.getIsDefault()).getCode());
            needUpdate = true;
        }

        if (dto.getIsEnabled() != null) {
            rule.setIsEnabled(EnableStatusEnum.EnableStatusfromBoolean(dto.getIsEnabled()).getCode());
            needUpdate = true;
        }

        if (StringUtils.hasText(dto.getVenueRefundRuleDesc())) {
            rule.setVenueRefundRuleDesc(dto.getVenueRefundRuleDesc());
            needUpdate = true;
        }

        if (needUpdate) {
            refundRuleMapper.updateById(rule);
            log.info("退款规则基本信息更新成功 - ruleId: {}", rule.getVenueRefundRuleId());
        }

        // 3. 如果提供了新的明细列表，更新明细
        if (dto.getDetails() != null && !dto.getDetails().isEmpty()) {
            validateRuleDetails(dto.getDetails());

            // 逻辑删除原有明细
            refundRuleDetailMapper.deleteByRuleId(dto.getVenueRefundRuleId());

            // 插入新明细
            List<VenueRefundRuleDetail> details = new ArrayList<>();
            for (int i = 0; i < dto.getDetails().size(); i++) {
                RefundRuleDetailDto detailDto = dto.getDetails().get(i);

                VenueRefundRuleDetail detail = VenueRefundRuleDetail.builder()
                        .venueRefundRuleId(rule.getVenueRefundRuleId())
                        .minHoursBefore(detailDto.getMinHoursBefore())
                        .maxHoursBefore(detailDto.getMaxHoursBefore())
                        .refundPercentage(detailDto.getRefundPercentage())
                        .handlingFeePercentage(detailDto.getHandlingFeePercentage())
                        .refundRuleDetailDesc(detailDto.getRefundRuleDetailDesc())
                        .sortOrderNum(detailDto.getSortOrderNum() != null ? detailDto.getSortOrderNum() : i)
                        .build();

                details.add(detail);
            }

            refundRuleDetailMapper.batchInsert(details);
            log.info("退款规则明细更新成功 - ruleId: {}, detailCount: {}",
                    rule.getVenueRefundRuleId(), details.size());
        }

        return getRefundRule(merchantId, rule.getVenueRefundRuleId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRefundRule(Long merchantId, Long ruleId) {
        log.info("删除退款规则 - merchantId: {}, ruleId: {}", merchantId, ruleId);

        // 1. 验证规则归属
        VenueRefundRule rule = validateRuleBelongsToMerchant(merchantId, ruleId);

        // 2. 检查是否有场馆正在使用该规则
        long normalCount = countVenuesUsingRule(ruleId, false);
        long activityCount = countVenuesUsingRule(ruleId, true);

        if (normalCount > 0 || activityCount > 0) {
            throw new GloboxApplicationException(
                    String.format("该退款规则正在被使用（普通退款: %d个场馆，活动退款: %d个场馆），无法删除",
                            normalCount, activityCount));
        }

        // 3. 逻辑删除规则明细
        refundRuleDetailMapper.deleteByRuleId(ruleId);

        // 4. 逻辑删除规则
        rule.setIsEnabled(EnableStatusEnum.DISABLED.getCode());
        refundRuleMapper.updateById(rule);

        log.info("退款规则删除成功（逻辑删除）- ruleId: {}", ruleId);
    }

    @Override
    public RefundRuleVo getRefundRule(Long merchantId, Long ruleId) {
        log.info("查询退款规则详情 - merchantId: {}, ruleId: {}", merchantId, ruleId);

        // 验证规则归属
        VenueRefundRule rule = validateRuleBelongsToMerchant(merchantId, ruleId);

        // 查询明细
        List<VenueRefundRuleDetail> details = refundRuleDetailMapper.selectByRuleId(ruleId);

        // 统计使用该规则的场馆数量
        int normalCount = countVenuesUsingRule(ruleId, false);
        int activityCount = countVenuesUsingRule(ruleId, true);

        return convertToVo(rule, details, normalCount, activityCount);
    }

    @Override
    public Page<RefundRuleVo> queryRefundRules(Long merchantId, QueryRefundRuleDto dto) {
        log.info("分页查询退款规则 - merchantId: {}, pageNum: {}, pageSize: {}",
                merchantId, dto.getPageNum(), dto.getPageSize());

        Page<VenueRefundRule> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        LambdaQueryWrapper<VenueRefundRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VenueRefundRule::getMerchantId, merchantId);

        // 场馆ID筛选
        if (dto.getVenueId() != null) {
            wrapper.eq(VenueRefundRule::getVenueId, dto.getVenueId());
        }

        // 默认规则筛选
        if (dto.getIsDefault() != null) {
            wrapper.eq(VenueRefundRule::getIsDefault,
                    DefaultStatusEnum.DefaultStatusfromBoolean(dto.getIsDefault()).getCode());
        }

        // 启用状态筛选
        if (dto.getIsEnabled() != null) {
            wrapper.eq(VenueRefundRule::getIsEnabled,
                    EnableStatusEnum.EnableStatusfromBoolean(dto.getIsEnabled()).getCode());
        }

        // 规则名称模糊查询
        if (StringUtils.hasText(dto.getVenueRefundRuleName())) {
            wrapper.like(VenueRefundRule::getVenueRefundRuleName, dto.getVenueRefundRuleName());
        }

        // 排序：默认规则优先，然后按创建时间倒序
        wrapper.orderByDesc(VenueRefundRule::getIsDefault)
                .orderByDesc(VenueRefundRule::getCreatedAt);

        Page<VenueRefundRule> rulePage = refundRuleMapper.selectPage(page, wrapper);

        // 转换为VO
        Page<RefundRuleVo> resultPage = new Page<>(dto.getPageNum(), dto.getPageSize());
        resultPage.setTotal(rulePage.getTotal());
        resultPage.setRecords(rulePage.getRecords().stream()
                .map(rule -> {
                    List<VenueRefundRuleDetail> details = refundRuleDetailMapper.selectByRuleId(
                            rule.getVenueRefundRuleId());
                    int normalCount = countVenuesUsingRule(rule.getVenueRefundRuleId(), false);
                    int activityCount = countVenuesUsingRule(rule.getVenueRefundRuleId(), true);
                    return convertToVo(rule, details, normalCount, activityCount);
                })
                .collect(Collectors.toList()));

        return resultPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindRefundRuleToVenue(Long merchantId, BindRefundRuleDto dto, boolean isActivityRule) {
        log.info("绑定退款规则到场馆 - merchantId: {}, venueId: {}, ruleId: {}, isActivityRule: {}",
                merchantId, dto.getVenueId(), dto.getVenueRefundRuleId(), isActivityRule);

        // 1. 验证规则归属
        validateRuleBelongsToMerchant(merchantId, dto.getVenueRefundRuleId());

        // 2. 验证场馆归属
        Venue venue = validateVenueBelongsToMerchant(merchantId, dto.getVenueId());

        // 3. 绑定规则到场馆
        if (isActivityRule) {
            venue.setActivityRefundRuleId(dto.getVenueRefundRuleId());
        } else {
            venue.setVenueRefundRuleId(dto.getVenueRefundRuleId());
        }

        venueMapper.updateById(venue);

        log.info("退款规则绑定成功 - venueId: {}, ruleId: {}, type: {}",
                dto.getVenueId(), dto.getVenueRefundRuleId(),
                isActivityRule ? "活动退款" : "普通退款");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultRule(Long merchantId, Long ruleId) {
        log.info("设置默认退款规则 - merchantId: {}, ruleId: {}", merchantId, ruleId);

        // 验证规则归属
        VenueRefundRule rule = validateRuleBelongsToMerchant(merchantId, ruleId);

        // 取消其他默认规则
        clearDefaultRules(merchantId, rule.getVenueId());

        // 设置为默认规则
        rule.setIsDefault(DefaultStatusEnum.DEFAULT.getCode());
        refundRuleMapper.updateById(rule);

        log.info("默认退款规则设置成功 - ruleId: {}", ruleId);
    }

    // ==================== 私有方法 ====================

    /**
     * 验证规则归属
     */
    private VenueRefundRule validateRuleBelongsToMerchant(Long merchantId, Long ruleId) {
        VenueRefundRule rule = refundRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new GloboxApplicationException("退款规则不存在");
        }
        if (!rule.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该退款规则");
        }
        return rule;
    }

    /**
     * 验证场馆归属
     */
    private Venue validateVenueBelongsToMerchant(Long merchantId, Long venueId) {
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null) {
            throw new GloboxApplicationException("场馆不存在");
        }
        if (!venue.getMerchantId().equals(merchantId)) {
            throw new GloboxApplicationException("无权操作该场馆");
        }
        return venue;
    }

    /**
     * 验证规则明细
     */
    private void validateRuleDetails(List<RefundRuleDetailDto> details) {
        if (details == null || details.isEmpty()) {
            throw new GloboxApplicationException("退款规则明细不能为空");
        }

        // 按最小提前小时数排序
        details.sort(Comparator.comparing(RefundRuleDetailDto::getMinHoursBefore));

        for (int i = 0; i < details.size(); i++) {
            RefundRuleDetailDto detail = details.get(i);

            // 验证退款比例
            if (detail.getRefundPercentage().compareTo(BigDecimal.ZERO) < 0 ||
                    detail.getRefundPercentage().compareTo(new BigDecimal("100")) > 0) {
                throw new GloboxApplicationException("退款比例必须在0-100之间");
            }

            // 验证手续费比例
            if (detail.getHandlingFeePercentage() != null &&
                    (detail.getHandlingFeePercentage().compareTo(BigDecimal.ZERO) < 0 ||
                            detail.getHandlingFeePercentage().compareTo(new BigDecimal("100")) > 0)) {
                throw new GloboxApplicationException("手续费比例必须在0-100之间");
            }

            // 验证时间范围
            if (detail.getMaxHoursBefore() != null &&
                    detail.getMinHoursBefore() >= detail.getMaxHoursBefore()) {
                throw new GloboxApplicationException(
                        String.format("时间范围无效：最小提前小时数(%d)必须小于最大提前小时数(%d)",
                                detail.getMinHoursBefore(), detail.getMaxHoursBefore()));
            }

            // 检查时间范围是否重叠
            for (int j = i + 1; j < details.size(); j++) {
                RefundRuleDetailDto other = details.get(j);

                // 检查是否有重叠
                boolean overlap = false;
                if (detail.getMaxHoursBefore() == null || other.getMaxHoursBefore() == null) {
                    // 如果有一个没有上限，检查另一个的下限
                    if (detail.getMaxHoursBefore() == null && other.getMinHoursBefore() < detail.getMinHoursBefore()) {
                        overlap = true;
                    }
                } else {
                    // 都有上限，检查范围重叠
                    overlap = !(detail.getMaxHoursBefore() <= other.getMinHoursBefore() ||
                            other.getMaxHoursBefore() <= detail.getMinHoursBefore());
                }

                if (overlap) {
                    throw new GloboxApplicationException(
                            String.format("时间范围存在重叠：[%d-%s] 与 [%d-%s]",
                                    detail.getMinHoursBefore(),
                                    detail.getMaxHoursBefore() != null ? detail.getMaxHoursBefore().toString() : "∞",
                                    other.getMinHoursBefore(),
                                    other.getMaxHoursBefore() != null ? other.getMaxHoursBefore().toString() : "∞"));
                }
            }
        }
    }

    /**
     * 清除默认规则
     */
    private void clearDefaultRules(Long merchantId, Long venueId) {
        LambdaQueryWrapper<VenueRefundRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VenueRefundRule::getMerchantId, merchantId)
                .eq(VenueRefundRule::getIsDefault, DefaultStatusEnum.DEFAULT.getCode());

        if (venueId != null) {
            wrapper.eq(VenueRefundRule::getVenueId, venueId);
        } else {
            wrapper.isNull(VenueRefundRule::getVenueId);
        }

        List<VenueRefundRule> rules = refundRuleMapper.selectList(wrapper);
        for (VenueRefundRule rule : rules) {
            rule.setIsDefault(DefaultStatusEnum.NOT_DEFAULT.getCode());
            refundRuleMapper.updateById(rule);
        }
    }

    /**
     * 统计使用该规则的场馆数量
     */
    private int countVenuesUsingRule(Long ruleId, boolean isActivityRule) {
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<>();
        if (isActivityRule) {
            wrapper.eq(Venue::getActivityRefundRuleId, ruleId);
        } else {
            wrapper.eq(Venue::getVenueRefundRuleId, ruleId);
        }
        return venueMapper.selectCount(wrapper).intValue();
    }

    /**
     * 转换为VO
     */
    private RefundRuleVo convertToVo(VenueRefundRule rule, List<VenueRefundRuleDetail> details,
                                     int normalVenueCount, int activityVenueCount) {
        // 查询场馆名称
        String venueName = null;
        if (rule.getVenueId() != null) {
            Venue venue = venueMapper.selectById(rule.getVenueId());
            if (venue != null) {
                venueName = venue.getName();
            }
        }

        // 转换明细
        List<RefundRuleVo.RefundRuleDetailVo> detailVos = details.stream()
                .sorted(Comparator.comparing(VenueRefundRuleDetail::getSortOrderNum))
                .map(this::convertDetailToVo)
                .collect(Collectors.toList());

        return RefundRuleVo.builder()
                .venueRefundRuleId(rule.getVenueRefundRuleId())
                .merchantId(rule.getMerchantId())
                .venueId(rule.getVenueId())
                .venueName(venueName)
                .venueRefundRuleName(rule.getVenueRefundRuleName())
                .isDefault(rule.getIsDefault() == 1)
                .isEnabled(rule.getIsEnabled() == 1)
                .venueRefundRuleDesc(rule.getVenueRefundRuleDesc())
                .normalVenueCount(normalVenueCount)
                .activityVenueCount(activityVenueCount)
                .details(detailVos)
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    /**
     * 转换明细为VO
     */
    private RefundRuleVo.RefundRuleDetailVo convertDetailToVo(VenueRefundRuleDetail detail) {
        String timeRangeDesc = buildTimeRangeDesc(detail.getMinHoursBefore(), detail.getMaxHoursBefore());

        return RefundRuleVo.RefundRuleDetailVo.builder()
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
            return String.format("提前%d小时以上", minHours);
        } else {
            return String.format("提前%d-%d小时", minHours, maxHours);
        }
    }
}