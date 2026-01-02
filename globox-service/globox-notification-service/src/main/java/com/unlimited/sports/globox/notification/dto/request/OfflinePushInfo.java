package com.unlimited.sports.globox.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 离线推送信息配置
 * 说明: 所有推送接口共用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfflinePushInfo {

    /**
     * 推送标志
     * 0: 进行离线推送
     * 1: 不进行离线推送（仅在线推送）
     */
    @JsonProperty("PushFlag")
    private Integer pushFlag;

    /**
     * 离线推送标题
     */
    @JsonProperty("Title")
    private String title;

    /**
     * 离线推送内容
     */
    @JsonProperty("Desc")
    private String desc;

    /**
     * 透传字段（JSON格式字符串）
     * 示例: "{\"entity\":{\"key1\":\"value1\",\"key2\":\"value2\"}}"
     * 用户看不到,给app看的数据
     */
    @JsonProperty("Ext")
    private String ext;
}
