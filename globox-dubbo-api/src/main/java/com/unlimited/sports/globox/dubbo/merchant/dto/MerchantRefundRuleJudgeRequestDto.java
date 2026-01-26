package com.unlimited.sports.globox.dubbo.merchant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @since 2026/1/3 17:27
 * 判断退款规则DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantRefundRuleJudgeRequestDto implements Serializable {


    @NotNull(message = "场馆ID不能为空")
    private Long venueId;

    @NotNull(message = "活动开始时间不能为空")
    private LocalDateTime eventStartTime;

    @NotNull(message = "退款申请时间不能为空")
    private LocalDateTime refundApplyTime;

    @NotNull(message = "下单时间不能为空")
    private LocalDateTime orderTime;

    private boolean isActivity;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

}
