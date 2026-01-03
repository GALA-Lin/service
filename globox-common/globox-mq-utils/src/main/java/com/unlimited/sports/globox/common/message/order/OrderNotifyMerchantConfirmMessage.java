package com.unlimited.sports.globox.common.message.order;

import com.unlimited.sports.globox.common.enums.order.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 通知商家确认订单消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderNotifyMerchantConfirmMessage implements Serializable {

    @NotNull
    private Long orderNo;

    @NotNull
    private Long venueId;

    @NotNull
    private OrderStatusEnum currentOrderStatus;
}
