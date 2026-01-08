package com.unlimited.sports.globox.common.enums.notification;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 用户类型枚举
 */
@Getter
@AllArgsConstructor
public enum PushUserTypeEnum {

    /**
     * 消费者
     */
    CONSUMER(1, "CONSUMER", "消费者"),

    /**
     * 商家
     */
    MERCHANT(2, "MERCHANT", "商家"),

    /**
     * 教练
     */
    COACH(3, "COACH", "教练");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String value;

    private final String description;

    /**
     * 根据code获取枚举
     */
    public static PushUserTypeEnum fromCode(Integer code) {
        return code == null ? null : Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElse(null);
    }

}
