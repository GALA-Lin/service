package com.unlimited.sports.globox.notification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 推送事件类型枚举
 * 对应腾讯云回调中的EventType字段
 */
@Getter
@AllArgsConstructor
public enum PushEventType {

    /**
     * 离线推送事件（推送送达）
     * 用户离线或未在线时，推送到厂商离线仓库
     */
    OFFLINE_PUSH(1, "OFFLINE_PUSH", "离线推送事件"),

    /**
     * 在线推送事件
     * 用户在线时，推送直接送达设备
     */
    ONLINE_PUSH(2, "ONLINE_PUSH", "在线推送事件"),

    /**
     * 推送点击事件
     * 用户点击了推送通知
     */
    PUSH_CLICK(3, "PUSH_CLICK", "推送点击事件");

    /**
     * 事件类型码
     */
    private final Integer code;

    /**
     * 事件类型名称
     */
    private final String name;

    /**
     * 事件描述
     */
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static PushEventType fromCode(Integer code) {
        return code == null ? null : Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 判断是否是送达事件（离线或在线）
     */
    public boolean isDeliveryEvent() {
        return this == OFFLINE_PUSH || this == ONLINE_PUSH;
    }

    /**
     * 判断是否是点击事件
     */
    public boolean isClickEvent() {
        return this == PUSH_CLICK;
    }
}
