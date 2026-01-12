package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 应用相关响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ApplicationCode implements ResultCode {
    SUCCESS(200,"处理成功"),
    FAIL(500,"处理失败"),
    FILE_SIZE_EXCEEDED(501,"达到文件上传限制")
    ;
    private final Integer code;
    private final String message;
}
