package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 教练确认订单 - 请求 dto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachConfirmRequestDto {

    @NotNull
    private Long orderNo;

    /**
     * 是否自动确认
     */
    @NotNull
    private boolean isAutoConfirm;

    /**
     * 如果不是自动确认，传入教练 ID
     */
    private Long coachId;
}
