package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

/**
 * @since 2026/1/2 12:55
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantRefundRuleQueryRequestDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 场馆ID（必填）
     * 用于优先查询场馆专属规则
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;
}