package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 校验相关响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ValidCode implements ResultCode {

    MISSING_REQUEST_PARAM(601,"请求参数缺失"),
    INVALID_REQUEST_PARAM(602,"无效请求参数"),
    ;
    private final Integer code;
    private final String message;
}