package com.unlimited.sports.globox.dubbo.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoachCancelOrderRequestDto implements Serializable {

    /**
     * 订单号
     */
    @NotNull(message = "订单号不能为空")
    private Long orderNo;

    /**
     * Venue ID（场馆）
     */
    @NotNull(message = "coach ID不能为空")
    private Long coachId;
}
