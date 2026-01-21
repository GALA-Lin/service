package com.unlimited.sports.globox.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 内容治理服务 错误码
 * 8000 - 8999
 */
@Getter
@AllArgsConstructor
public enum GovernanceCode implements ResultCode{
    MANIFEST_EMPTY(8001, "敏感词 manifest.json 文件为空或不存在"),
    SENSITIVE_WORDS_DOWNLOAD_FAILED(8002, "敏感词文件下载失败"),
    SENSITIVE_WORDS_RELOAD_ERROR(8003, "敏感词载入失败"),
    CONTENT_NOT_ALLOW(8004, "存在敏感词，请修改后重试"),
    REPORT_CREATE_FAILED(8005, "举报工单创建失败"),
    REPORT_TARGET_TYPE_INVALID(8006, "举报对象类型无效"),
    REPORT_REASON_INVALID(8007, "举报原因无效"),
    REPORT_SNAPSHOT_CREATE_FAILED(8008, "举报工单快照创建失败"),
    REPORT_EVIDENCE_CREATE_FAILED(8009, "举报证据保存失败");



    private final Integer code;
    private final String message;
}
