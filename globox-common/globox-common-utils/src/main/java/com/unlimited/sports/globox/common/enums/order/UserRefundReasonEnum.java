package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户退款原因枚举
 */
@Getter
@AllArgsConstructor
public enum UserRefundReasonEnum {


    NONE(0,"无"),

    CHANGE_OF_MIND(1, "改变主意，不想预订了"),

    SCHEDULE_CONFLICT(2, "时间冲突，无法到场"),

    VENUE_ISSUE(3, "场馆问题"),

    DUPLICATE_PAYMENT(4, "重复付款"),

    SERVICE_QUALITY_ISSUE(5, "服务质量问题"),

    WEATHER_REASON(6, "天气原因"),

    EMERGENCY(7, "个人紧急事件"),

    OTHER(8, "其他原因");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    /**
     * 根据 code 解析枚举
     */
    public static UserRefundReasonEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserRefundReasonEnum reason : values()) {
            if (reason.code == code) {
                return reason;
            }
        }
        return null;
    }
}
