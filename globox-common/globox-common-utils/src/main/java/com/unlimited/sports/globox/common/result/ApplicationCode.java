package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 应用相关响应码枚举
 *
 * @author dk
 * @since 2025/12/17 22:04
 */
@Getter
@AllArgsConstructor
public enum ApplicationCode implements ResultCode {
    SUCCESS(200,"处理成功"),
    FAIL(500,"处理失败"),
    ;
    private final Integer code;
    private final String message;
}
