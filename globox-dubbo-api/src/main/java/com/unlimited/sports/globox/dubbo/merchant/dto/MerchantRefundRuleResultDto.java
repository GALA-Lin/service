package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @since 2026/1/2 12:50
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundRuleResultDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 退款规则ID
     */
    @NotNull
    private Long venueRefundRuleId;

    /**
     * 商家ID
     */
    @NotNull
    private Long merchantId;

    /**
     * 场馆ID（如果是商家默认规则则为null）
     */
    private Long venueId;

    /**
     * 规则名称
     */
    @NotNull
    private String venueRefundRuleName;

    /**
     * 是否为默认规则
     */
    @NotNull
    private Boolean isDefault;

    /**
     * 是否启用
     */
    @NotNull
    private Boolean isEnabled;

    /**
     * 规则描述
     */
    private String venueRefundRuleDesc;

    /**
     * 退款规则明细列表（按时间阶梯排序）
     */
    @NotNull
    private List<MerchantRefundRuleDetailDto> details;

    /**
     * 创建时间
     */
    @NotNull
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @NotNull
    private LocalDateTime updatedAt;
}