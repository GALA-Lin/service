package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/27 14:53
 * 槽位记录状态枚举
 */
@Getter
@AllArgsConstructor
public enum SlotRecordStatusEnum {

    AVAILABLE(1, "可预订", "时段空闲，可以下单"),
    LOCKED_IN(2, "占用中", "已下单但未完成支付，临时锁定"),
    UNAVAILABLE(3, "不可预订", "商家禁用或特殊情况关闭");

    private final Integer code;
    private final String name;
    private final String desc;

    public static SlotRecordStatusEnum getByCode(Integer code) {
        for (SlotRecordStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
