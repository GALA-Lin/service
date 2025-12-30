package com.unlimited.sports.globox.model.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 推送状态枚举
 */
@Getter
@AllArgsConstructor
public enum PushStatusEnum {

    /**
     * 待发送
     */
    PENDING(0, "PENDING", "待发送"),

    /**
     * 已发送
     */
    SENT(1, "SENT", "已发送"),

    /**
     * 已送达
     */
    DELIVERED(2, "DELIVERED", "已送达"),

    /**
     * 发送失败
     */
    FAILED(3, "FAILED", "失败"),

    /**
     * 已过滤（用户未激活）
     */
    FILTERED(4, "FILTERED", "已过滤");

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String value;

    private final String description;



}
