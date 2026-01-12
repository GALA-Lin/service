package com.unlimited.sports.globox.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 全部已读请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAllReadRequest {

    /**
     * 消息类型（可选）
     * explore=探索消息, rally=球局消息, system=系统消息
     * 不传则全部类型已读
     */
    private String messageType;
}
