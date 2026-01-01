package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderActionEnum {
    CREATE(1, "CREATE"),
    PAY(2, "PAY"),
    CONFIRM(3, "CONFIRM"),
    COMPLETE(4, "COMPLETE"),
    CANCEL(5, "CANCEL"),
    REFUND_APPLY(6, "REFUND_APPLY"),
    REFUND_APPROVE(7, "REFUND_APPROVE"),
    REFUND_REJECT(8, "REFUND_REJECT"),
    REFUND_COMPLETE(9, "REFUND_COMPLETE"),
    REFUND_CANCEL(10, "REFUND_CANCEL"),
    SYSTEM_ADJUST(11, "SYSTEM_ADJUST"),
    ;


    @EnumValue
    @JsonValue
    private final int code;
    private final String description;
}