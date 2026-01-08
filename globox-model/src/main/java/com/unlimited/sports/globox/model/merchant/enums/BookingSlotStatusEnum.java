package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025-12-18-11:28
 * 预订槽位状态枚举
 */
@Getter
@AllArgsConstructor
public enum BookingSlotStatusEnum {

    BOOKABLE(1, "可预订", "场馆空闲，支持下单预订"),
    OCCUPIED(2, "占用中", "场地临时占用，支付中/未被商家确定"),
    CLOSED(3, "关闭", "场馆禁用/时段取消，不可操作");

    private final Integer code;
    private final String name;
    private final String desc;

    public static BookingSlotStatusEnum getByCode(Integer code) {
        for (BookingSlotStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}