package com.unlimited.sports.globox.dubbo.order.dto;

import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import java.time.LocalDateTime;

/**
 * 商家确认订单 rpc - 结果类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantConfirmResultDto {
    /**
     * 订单号
     */
    @NotNull
    private Long orderNo;

    /**
     * 是否取消成功
     */
    @NotNull
    private boolean success;

    /**
     * 当前订单状态
     */
    @NotNull
    private OrderStatusEnum orderStatus;

    /**
     * 状态描述
     */
    @NotNull
    private String orderStatusName;

    /**
     * 取消时间
     */
    @Null
    private LocalDateTime confirmAt;
}
