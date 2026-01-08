package com.unlimited.sports.globox.model.merchant.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @since 2025/12/31 11:37
 * 默认情况枚举
 */
@Getter
@AllArgsConstructor
public enum DefaultStatusEnum {
    // 默认
    DEFAULT(1, "默认"),
    // 非默认
    NOT_DEFAULT(0, "非默认");

    private final Integer code;
    private final String desc;

    public static DefaultStatusEnum DefaultStatusfromBoolean(Boolean defaultStatus) {
        return Boolean.TRUE.equals(defaultStatus) ? DEFAULT : NOT_DEFAULT;
    }


    public static DefaultStatusEnum getDefaultStatusEnum(Integer code) {
        for (DefaultStatusEnum statusEnum : DefaultStatusEnum.values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }

}
