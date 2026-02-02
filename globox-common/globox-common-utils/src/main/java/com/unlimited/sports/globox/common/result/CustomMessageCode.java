package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 自定义错误码
 */
@Data
@AllArgsConstructor
public class CustomMessageCode implements ResultCode {

    private final Integer code;
    private final String message;

    public static CustomMessageCode create(ResultCode code, String message) {
        return new CustomMessageCode(code.getCode(), message);
    }

    @Override
    public String name() {
        return "custom code";
    }
}
