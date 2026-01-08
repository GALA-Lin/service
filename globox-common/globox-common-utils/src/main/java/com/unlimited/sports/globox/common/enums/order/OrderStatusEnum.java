package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatusEnum {
    /**
     * 订单待处理
     */
    PENDING(1, "PENDING"),

    /**
     * 订单已支付
     */
    PAID(2, "PAID"),

    /**
     * 订单已确认
     */
    CONFIRMED(3, "CONFIRMED"),

    /**
     * 订单已完成
     */
    COMPLETED(4, "COMPLETED"),

    /**
     * 订单已取消（未支付）
     */
    CANCELLED(5, "CANCELLED"),

    /**
     * 订单申请退款中
     */
    REFUND_APPLYING(6, "REFUND_APPLYING"),

    /**
     * 订单正在退款中
     */
    REFUNDING(7,"REFUNDING"),

    /**
     * 部分退款完成
     */
    PARTIALLY_REFUNDED(8, "PARTIALLY_REFUNDED"),

    /**
     * 订单已退款
     */
    REFUNDED(9, "REFUNDED"),

    /**
     * 退款已拒绝
     */
    REFUND_REJECTED(10, "REFUND_REJECTED"),

    /**
     * 退款申请已取消
     */
    REFUND_CANCELLED(11, "REFUND_CANCELLED")
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
