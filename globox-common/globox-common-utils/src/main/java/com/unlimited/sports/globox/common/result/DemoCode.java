package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 演示用例响应码枚举
 *
 * @author dk
 * @since 2025/12/17 18:59
 */
@Getter
@AllArgsConstructor
public enum DemoCode implements ResultCode {
    USER_NOT_EXIST(1101, "用户不存在"),
    USER_INSERT_ERROR(1102, "用户添加失败"),
    USER_UPDATE_ERROR(1103, "用户修改失败"),
    USER_DELETE_ERROR(1104, "用户删除失败"),
    ORDER_CREATE_FAILED(1105, "订单创建失败"),
    ACCOUNT_DEDUCT_FAILED(1106, "账户余额扣除失败")
    ;
    private final Integer code;
    private final String message;
}
