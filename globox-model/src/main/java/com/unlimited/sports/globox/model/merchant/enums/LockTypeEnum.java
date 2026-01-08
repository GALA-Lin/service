package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/29 13:30
 * 锁定类型枚举
 */
@Getter
@AllArgsConstructor
public enum LockTypeEnum {

    USER_ORDER(1, "用户订单"),
    MERCHANT_LOCK(2, "商家锁场");

    private final Integer code;
    private final String desc;

    /**
     * 根据编码获取枚举
     */
    public static LockTypeEnum getByCode(Integer code) {
        for (LockTypeEnum type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}