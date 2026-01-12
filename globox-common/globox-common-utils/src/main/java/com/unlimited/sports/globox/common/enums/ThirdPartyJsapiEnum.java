package com.unlimited.sports.globox.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 第三方小程序 枚举
 */
@Getter
@AllArgsConstructor
public enum ThirdPartyJsapiEnum {
    MOON_COURT(1,"揽月")
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
