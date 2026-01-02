package com.unlimited.sports.globox.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 推送响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PushResponse extends TencentImResponse {

    /**
     * 推送任务 ID
     */
    @JsonProperty("TaskId")
    private String taskId;
}
