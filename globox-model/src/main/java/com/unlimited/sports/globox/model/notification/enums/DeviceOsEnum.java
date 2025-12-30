package com.unlimited.sports.globox.model.notification.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备操作系统枚举
 */
@Getter
@AllArgsConstructor
public enum DeviceOsEnum {

    /**
     * iOS操作系统
     */
    iOS(1, "iOS", "iOS"),

    /**
     * Android操作系统
     */
    ANDROID(2, "Android", "Android"),


    HARMONY_OS(3, "HarmonyOS", "HarmonyOS")
    ;

    @EnumValue
    private final Integer code;

    @JsonValue
    private final String value;

    private final String description;

}
