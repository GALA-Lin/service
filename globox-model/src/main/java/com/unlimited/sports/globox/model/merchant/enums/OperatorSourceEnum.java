package com.unlimited.sports.globox.model.merchant.enums;

import com.unlimited.sports.globox.model.merchant.entity.Merchant;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/29 13:31
 * 操作人来源枚举
 */
@Getter
@AllArgsConstructor
public enum OperatorSourceEnum {

    MERCHANT(1, "商家端"),
    USER(2, "用户端");


    private final Integer code;
    private final String desc;
    /**
     * 根据编码获取枚举
     */
    public static OperatorSourceEnum getByCode(Integer code) {
        for (OperatorSourceEnum source : values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        return null;
    }

}
