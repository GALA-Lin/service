package com.unlimited.sports.globox.common.enums.user;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

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

    /**
     * 根据code获取枚举
     */
    public static DeviceOsEnum fromCode(Integer code) {
        return code == null ? null : Arrays.stream(values())
                .filter(os -> os.code.equals(code))
                .findFirst()
                .orElse(null);
    }

}
