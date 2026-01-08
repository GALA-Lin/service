package com.unlimited.sports.globox.common.message.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 订单通知商家 订单已变为已支付状态事件 - payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaidMessage implements Serializable {
    private Long orderNo;
    private Long userId;
    private List<Long> recordIds;
    private Boolean isActivity;
}
