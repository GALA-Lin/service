package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @since 2026/1/3 17:43
 * 退款判断Vo
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantRefundRuleJudgeResultVo implements Serializable {

    /**
     * 是否可以退款
     */
    private boolean canRefund;

    /**
     * 退款比例（百分比）
     * 例如：100.00 表示100%
     */
    private BigDecimal refundPercentage;

    /**
     * 不可退款原因（canRefund为false时有效）
     */
    private String reason;

}
