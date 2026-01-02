package com.unlimited.sports.globox.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 腾讯云IM API 通用响应
 */
@Data
public class TencentImResponse {

    /**
     * 请求处理的结果
     * OK: 表示处理成功
     * FAIL: 表示失败
     */
    @JsonProperty("ActionStatus")
    private String actionStatus;

    /**
     * 错误码
     * 0 表示成功，非0表示失败
     */
    @JsonProperty("ErrorCode")
    private Integer errorCode;

    /**
     * 错误信息
     */
    @JsonProperty("ErrorInfo")
    private String errorInfo;

    /**
     * 判断请求是否成功
     */
    public boolean isSuccess() {
        return "OK".equals(actionStatus) && errorCode != null && errorCode == 0;
    }
}
