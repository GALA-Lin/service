package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 额外费用计费方式 - 枚举
 * 计费方式：1=FIXED，2=PERCENTAGE
 */
@Getter
@AllArgsConstructor
public enum ChargeModeEnum {

    FIXED(1, "FIXED"),
    PERCENTAGE(2, "PERCENTAGE"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;

    /**
     * 根据code值获取对应的枚举
     *
     * @param code 计费方式代码
     * @return 对应的ChargeModeEnum，未找到时返回null
     */
    public static ChargeModeEnum getByCode(Integer code) {
        return Arrays.stream(ChargeModeEnum.values())
                .filter(mode -> mode.getCode().equals(code))
                .findFirst()
                .orElse(null);
    }
}
