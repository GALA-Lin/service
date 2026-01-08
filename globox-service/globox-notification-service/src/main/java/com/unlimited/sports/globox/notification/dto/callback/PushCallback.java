package com.unlimited.sports.globox.notification.dto.callback;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 腾讯云推送事件回调
 * 包含推送送达、推送点击等事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushCallback {

    /**
     * 回调事件数组（长度范围1～100）
     */
    @JsonProperty("Events")
    private List<CallbackEvent> events;

    /**
     * 回调事件项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallbackEvent {

        /**
         * 回调命令
         * "Push.AllMemberPush" = 全员推送
         * "Push.TagPush" = 标签推送
         * "Push.SinglePush" = 单发推送
         */
        @JsonProperty("CallbackCommand")
        private String callbackCommand;

        /**
         * 事件类型
         * 1 = 离线推送事件（推送送达）
         * 2 = 在线推送事件
         * 3 = 推送点击事件（用户点击推送）
         */
        @JsonProperty("EventType")
        private Integer eventType;

        /**
         * 推送任务ID（全员/标签/单推推送的TaskId）
         */
        @JsonProperty("TaskId")
        private String taskId;

        /**
         * 推送任务发起时间戳（秒）
         */
        @JsonProperty("TaskTime")
        private Long taskTime;

        /**
         * 事件发生时间戳（秒）
         * 离线推送：用户接收到推送的时间
         * 点击事件：用户点击推送的时间
         */
        @JsonProperty("EventTime")
        private Long eventTime;

        /**
         * 接收者账号（UserID或RegistrationID）
         */
        @JsonProperty("To_Account")
        private String toAccount;

        /**
         * 推送厂商
         * 0 = 在线推送
         * 1 = APNS（苹果）
         * 2 = 华为
         * 3 = 荣耀
         * 4 = OPPO
         * 5 = vivo
         * 6 = 小米
         * 7 = 魅族
         * 8 = Google(FCM)
         */
        @JsonProperty("PushPlatform")
        private Integer pushPlatform;

        /**
         * 推送阶段
         * 1 = 推送到厂商成功（送达）
         * 2 = 推送到用户设备成功（点击）
         */
        @JsonProperty("PushStage")
        private Integer pushStage;

        /**
         * 推送事件结果码
         * 0 = 成功
         * 其他 = 失败
         */
        @JsonProperty("ErrCode")
        private Integer errCode;

        /**
         * 推送事件结果描述
         */
        @JsonProperty("ErrInfo")
        private String errInfo;
    }
}
