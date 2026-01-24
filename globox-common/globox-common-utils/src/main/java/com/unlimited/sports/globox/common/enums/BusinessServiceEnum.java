package com.unlimited.sports.globox.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务服务枚举
 */
@Getter
@AllArgsConstructor
public enum BusinessServiceEnum {
    UNKNOW(0, "未知服务"),
    MERCHANT(2,"商家/场地服务"),
    COACH(3, " 教练服务"),
    USER(4, "用户服务"),
    SOCIAL(5,"社交服务"),
    ORDER(6, "订单服务"),
    PAYMENT(7,"支付服务"),
    NOTIFICATION(8, "推送服务"),
    GOVERNANCE(9, "治理服务"),
    ;

    @JsonValue
    private final Integer code;
    private final String desc;

    public static BusinessServiceEnum of(Integer code) {
        if (code == null) return null;
        for (BusinessServiceEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}
