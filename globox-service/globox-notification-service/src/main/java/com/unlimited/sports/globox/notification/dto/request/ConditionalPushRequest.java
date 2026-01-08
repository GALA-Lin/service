package com.unlimited.sports.globox.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 条件推送请求（标签/属性）
 * API: /v4/timpush/push
 * 说明: 按标签或属性条件推送
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ConditionalPushRequest extends BasePushRequest {

    /**
     * 推送目标人群的筛选条件
     */
    @JsonProperty("Condition")
    private PushCondition condition;
}
