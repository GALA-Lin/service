package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025-12-18-13:22
 * 订场订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum VenueOrderStatusEnum {

    PENDING(1, "待处理", "订单已生成，等待确认"),
    CONFIRMED(2,  "已确认", "订单已确认，开始履约"),
    COMPLETED(3,"已完成", "订单履约完成，交易结束"),
    CANCELLED(4, "已取消", "订单已取消，交易终止");

    private final Integer code;
    private final String name;
    private final String desc;

    public static VenueOrderStatusEnum getByCode(Integer code) {
        for (VenueOrderStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }


}
