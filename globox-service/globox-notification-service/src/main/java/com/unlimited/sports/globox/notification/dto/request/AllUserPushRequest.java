package com.unlimited.sports.globox.notification.dto.request;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 全员推送请求
 * API: /v4/timpush/push
 * 说明: 向所有用户发送推送
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AllUserPushRequest extends BasePushRequest {

}
