package com.unlimited.sports.globox.model.merchant.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款规则VO
 *
 * @since 2025/12/31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRuleVo {

    /**
     * 规则ID
     */
    private Long venueRefundRuleId;

    /**
     * 商家ID
     */
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
    private Boolean isDefault;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 规则描述
     */
    private String venueRefundRuleDesc;

    /**
     * 使用该规则的场馆数量（作为普通退款规则）
     */
    private Integer normalVenueCount;

    /**
     * 使用该规则的场馆数量（作为活动退款规则）
     */
    private Integer activityVenueCount;

    /**
     * 退款规则明细列表
     */
    private List<RefundRuleDetailVo> details;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 退款规则明细VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRuleDetailVo {

        /**
         * 明细ID
         */
        private Long venueRefundRuleDetailId;

        /**
         * 最小提前小时数（包含）
         */
        private Integer minHoursBefore;

        /**
         * 最大提前小时数（不包含）
         */
        private Integer maxHoursBefore;

        /**
         * 退款比例（0-100）
         */
        private java.math.BigDecimal refundPercentage;

        /**
         * 手续费比例（0-100）
         */
        private java.math.BigDecimal handlingFeePercentage;

        /**
         * 说明文字
         */
        private String refundRuleDetailDesc;

        /**
         * 排序序号
         */
        private Integer sortOrderNum;

        /**
         * 时间范围描述（前端展示用）
         */
        private String timeRangeDesc;
    }
}