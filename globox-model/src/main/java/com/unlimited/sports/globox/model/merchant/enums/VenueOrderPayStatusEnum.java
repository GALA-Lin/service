package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025-12-18-13:35
 * 订场支付状态枚举
 */
@Getter
@AllArgsConstructor
public enum VenueOrderPayStatusEnum {
    UNPAID(1, "未支付"),
    PAID(2, "已支付"),
    PARTIALLY_REFUNDED(3, "部分退款"),
    REFUNDED(4, "全部退款");

    private final Integer code;
    private final String name;


    public static VenueOrderPayStatusEnum getByCode(Integer code) {
        for (VenueOrderPayStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

}
