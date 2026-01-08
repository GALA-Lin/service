package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @since 2026/1/2 12:53
 * 退款规则明细DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundRuleDetailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 明细ID
     */
    @NotNull
    private Long venueRefundRuleDetailId;

    /**
     * 最小提前小时数（包含）
     * 例如：24 表示提前24小时及以上
     */
    @NotNull
    private Integer minHoursBefore;

    /**
     * 最大提前小时数（不包含）
     * 例如：72 表示提前72小时以内
     * null表示无上限
     */
    private Integer maxHoursBefore;

    /**
     * 退款比例（0-100）
     * 例如：80 表示退款80%
     */
    @NotNull
    private BigDecimal refundPercentage;

    /**
     * 手续费比例（0-100）
     * 例如：5 表示收取5%手续费
     */
    @NotNull
    private BigDecimal handlingFeePercentage;

    /**
     * 明细描述
     */
    private String refundRuleDetailDesc;

    /**
     * 排序号
     */
    @NotNull
    private Integer sortOrderNum;

    /**
     * 时间范围描述（前端展示用）
     * 例如："24-72小时"、"72小时以上"
     */
    @NotNull
    private String timeRangeDesc;
}
