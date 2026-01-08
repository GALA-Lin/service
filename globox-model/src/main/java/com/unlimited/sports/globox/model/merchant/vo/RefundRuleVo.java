package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2025/12/31 11:07
 * 退款规则详情 Vo
 */
@Data
@Builder
public class RefundRuleVo {

    /**
     * 规则ID
     */
    @NonNull
    private Long venueRefundRuleId;

    /**
     * 商家ID
     */
    @NonNull
    private Long merchantId;

    /**
     * 场馆ID（NULL表示商家默认规则）
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

    /**
     * 规则名称
     */
    private String venueRefundRuleName;

    /**
     * 是否为默认规则
     */
    @NonNull
    private Boolean isDefault;

    /**
     * 是否启用
     */
    @NonNull
    private Boolean isEnabled;

    /**
     * 规则描述
     */
    private String venueRefundRuleDesc;

    /**
     * 退款规则明细列表
     */
    @NonNull
    private List<RefundRuleDetailVo> details;

    /**
     * 使用该规则的场馆数量
     */
    private Integer venueCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

