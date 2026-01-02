package com.unlimited.sports.globox.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * 单发/批量推送请求
 * API: /v4/timpush/batch
 * 说明: 接收方账号列表在 [1, 500] 个之间
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class BatchPushRequest extends BasePushRequest {

    /**
     * 接收方账号列表（支持 UserID 或 RegistrationID）
     * 数组大小范围: [1, 500]
     */
    @JsonProperty("To_Account")
    private List<String> toAccount;
}
