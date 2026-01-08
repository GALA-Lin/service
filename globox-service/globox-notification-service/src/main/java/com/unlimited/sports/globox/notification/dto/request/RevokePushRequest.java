package com.unlimited.sports.globox.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送撤回请求
 * API: /v4/timpush/revoke
 * 说明: 撤回24小时内的全员/标签推送任务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevokePushRequest {

    /**
     * 推送任务 ID
     */
    @JsonProperty("TaskId")
    private String taskId;

    /**
     * 撤回后的离线推送信息
     * 说明: 用于覆盖已下发的离线推送
     */
    @JsonProperty("OfflinePushInfo")
    private OfflinePushInfo offlinePushInfo;


}
