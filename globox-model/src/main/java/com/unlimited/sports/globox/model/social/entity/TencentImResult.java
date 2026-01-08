package com.unlimited.sports.globox.model.social.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 腾讯云IM返回结果封装
 */
@Data
public class TencentImResult implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 原始返回结果
     */
    private String rawResult;

    /**
     * 消息随机值
     */
    private Integer msgRandom;

    /**
     * 错误码
     */
    private Integer errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 消息时间戳
     */
    private Long msgTime;

    /**
     * 消息Key
     */
    private String msgKey;

    /**
     * 发送方账号
     */
    private String fromAccount;

    /**
     * 接收方账号
     */
    private String toAccount;

    /**
     * 扩展信息
     */
    private String extra;

    /**
     * 是否成功
     */
    private Boolean success;

    public TencentImResult() {
    }

    public TencentImResult(String rawResult, Integer msgRandom) {
        this.rawResult = rawResult;
        this.msgRandom = msgRandom;
    }

    /**
     * 判断是否成功
     * @return 是否成功
     */
    public boolean isSuccess() {
        return errorCode != null && errorCode == 0;
    }
}
