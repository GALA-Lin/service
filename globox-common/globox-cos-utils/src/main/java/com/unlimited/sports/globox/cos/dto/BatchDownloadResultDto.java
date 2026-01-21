package com.unlimited.sports.globox.cos.dto;

import lombok.AllArgsConstructor;

import java.util.Map;

/**
 * 批量下载结果：成功内容 + 失败原因
 */
public record BatchDownloadResultDto(Map<String, String> contents, Map<String, Throwable> errors) {
    public boolean allSuccess() {return errors.isEmpty();}
}