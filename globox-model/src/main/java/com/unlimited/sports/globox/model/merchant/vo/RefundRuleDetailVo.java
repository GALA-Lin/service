package com.unlimited.sports.globox.model.merchant.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @since 2025/12/31 11:09
 * 退款规则明细Vo
 */
@Data
@Builder
public class RefundRuleDetailVo {

    /**
     * 明细ID
     */
    @NonNull
    private Long venueRefundRuleDetailId;

    /**
     * 最小提前小时数
     */
    @NonNull
    private Integer minHoursBefore;

    /**
     * 最大提前小时数（NULL表示无上限）
     */
    private Integer maxHoursBefore;

    /**
     * 退款比例
     */
    @NonNull
    private BigDecimal refundPercentage;

    /**
     * 是否需要联系商家
     */
    @NonNull
    private Boolean needContactMerchant;

    /**
     * 手续费比例
     */
    @NonNull
    private BigDecimal handlingFeePercentage;

    /**
     * 说明文字
     */
    private String refundRuleDetailDesc;

    /**
     * 排序序号
     */
    @NonNull
    private Integer sortOrderNum;

    /**
     * 时间范围描述（前端展示用）
     */
    private String timeRangeDesc;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
