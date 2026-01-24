package com.unlimited.sports.globox.common.enums.governance;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 死信日志处理状态枚举
 */
@Getter
@AllArgsConstructor
public enum MQDeadLetterLogHandleStatusEnum {
    NEW(1, "采集完成"),
    REPLAYED(2, "重试"),
    IGNORED(3,"忽略"),
    RESOLVED(4,"已解决"),
    FAILED(5,"解决失败")
    ;

    @EnumValue
    @JsonValue
    private final Integer code;

    private final String desc;

    public static MQDeadLetterLogHandleStatusEnum of(Integer code) {
        if (code == null) return null;
        for (MQDeadLetterLogHandleStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return of(code) != null;
    }
}
