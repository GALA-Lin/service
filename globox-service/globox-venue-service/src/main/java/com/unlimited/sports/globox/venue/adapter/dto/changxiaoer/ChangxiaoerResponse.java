package com.unlimited.sports.globox.venue.adapter.dto.changxiaoer;

import lombok.Data;

/**
 * 场小二统一响应
 */
@Data
public class ChangxiaoerResponse<T> {

    /**
     * 是否成功
     */
    private Boolean flag;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 状态码
     */
    private String statusCode;

    /**
     * 响应数据
     */
    private T data;
}
