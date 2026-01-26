package com.unlimited.sports.globox.common.enums.payment;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分账状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProfitSharingStatusEnum {
    PENDING(1,"待处理"),
    PROCESSING(2, "进行中"),
    FINISHED(3,"完成"),
    FAILED(4,"处理失败"),
    ;
    @EnumValue
    @JsonValue
    private final Integer code;
    private final String description;
}
