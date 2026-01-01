package com.unlimited.sports.globox.common.enums.order;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 退款状态枚举
 */
@Getter
@AllArgsConstructor
public enum RefundStatusEnum {
    NONE(0, "NONE"),
    WAIT_APPROVING(1, "WAIT_APPROVING"),
    PENDING(2, "PENDING"),
    APPROVED(3, "APPROVED"),
    COMPLETED(4, "COMPLETED"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
