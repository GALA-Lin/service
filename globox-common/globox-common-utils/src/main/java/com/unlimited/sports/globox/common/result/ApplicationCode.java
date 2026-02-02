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
    FILE_SIZE_EXCEEDED(501,"达到文件上传限制"),
    FLOW(510, "限流"),
    DEGRADED(511, "降级"),
    SYSTEM_BLOCK(512, "系统限流"),
    PARAM_FLOW(513, "热点数据限流"),
    AUTHORITY(514, "权限限流"),
    GATEWAY_FLOW(515, "网关限流"),
    UNKNOW(99999, "未知的状态码"),

    ;
    private final Integer code;
    private final String message;
}
