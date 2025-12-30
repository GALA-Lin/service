package com.unlimited.sports.globox.model.venue.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 操作人来源枚举
 * 记录槽位操作的来源系统
 */
@Getter
@AllArgsConstructor
public enum OperatorSourceEnum {

    /**
     * 商家端操作（商家后台管理系统）
     */
    MERCHANT(1, "商家端"),

    /**
     * 用户端操作（用户提交订单）
     */
    USER(2, "用户端");

    @EnumValue
    private final Integer code;
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static OperatorSourceEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OperatorSourceEnum source : values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知的操作来源: " + code);
    }
}
