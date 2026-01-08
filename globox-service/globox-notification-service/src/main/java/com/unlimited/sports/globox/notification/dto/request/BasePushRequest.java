package com.unlimited.sports.globox.notification.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * 推送请求基类
 * 使用JsonProperty适配腾讯云的JSON的大驼峰命名形式
 */
@Data
@SuperBuilder
public abstract class BasePushRequest {

    /**
     * 消息推送方账号
     */
    @JsonProperty("From_Account")
    private String fromAccount;

    /**
     * 32位无符号整数随机数（用于消息去重）
     */
    @JsonProperty("MsgRandom")
    private Integer msgRandom;

    /**
     * 离线推送信息配置
     */
    @JsonProperty("OfflinePushInfo")
    private OfflinePushInfo offlinePushInfo;

    /**
     * 客户业务自定义标识（最大64字节） 主要用于回调
     */
    @JsonProperty("DataId")
    private String dataId;
}
