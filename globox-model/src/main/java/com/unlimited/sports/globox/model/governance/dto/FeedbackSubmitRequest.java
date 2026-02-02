package com.unlimited.sports.globox.model.governance.dto;

import lombok.Data;

import javax.validation.constraints.*;

/**
 * 用户提交反馈 - 请求DTO
 */
@Data
public class FeedbackSubmitRequest {

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 500, message = "反馈内容长度不能超过500")
    private String content;

    @Size(max = 64, message = "联系方式长度不能超过64")
    private String contact;

    @Size(max = 32, message = "App版本号长度不能超过32")
    private String appVersion;

    @Size(max = 32, message = "系统版本长度不能超过32")
    private String osVersion;

    @Size(max = 64, message = "设备型号长度不能超过64")
    private String deviceModel;
}