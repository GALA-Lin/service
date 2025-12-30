package com.unlimited.sports.globox.model.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型枚举
 */
@Getter
@AllArgsConstructor
public enum UserTypeEnum {

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



}
