package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

/**
 * @since 2025/12/31 11:09
 * 退款规则简要信息Vo (用于列表展示)
 */
@Data
@Builder
public class RefundRuleSimpleVo {

    /**
     * 规则ID
     */
    @NonNull
    private Long venueRefundRuleId;

    /**
     * 规则名称
     */
    private String venueRefundRuleName;

    /**
     * 场馆ID
     */
    private Long venueId;

    /**
     * 场馆名称
     */
    private String venueName;

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
     * 规则描述（简短）
     */
    private String venueRefundRuleDesc;

    /**
     * 规则摘要（如："24小时前100%，12小时前50%"）
     */
    @NonNull
    private String ruleSummary;

    /**
     * 明细数量
     */
    @NonNull
    private Integer detailCount;

    /**
     * 使用该规则的场馆数量
     */
    private Integer venueCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
