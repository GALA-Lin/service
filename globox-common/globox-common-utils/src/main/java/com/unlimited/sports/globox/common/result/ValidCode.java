package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 校验相关响应码枚举
 *
 * @author dk
 * @since 2025/12/17 22:05
 */
@Getter
@AllArgsConstructor
public enum ValidCode implements ResultCode {

    MISSING_REQUEST_PARAM(400,"请求参数缺失"),
    INVALID_REQUEST_PARAM(401,"无效请求参数"),
    ;
    private final Integer code;
    private final String message;
}