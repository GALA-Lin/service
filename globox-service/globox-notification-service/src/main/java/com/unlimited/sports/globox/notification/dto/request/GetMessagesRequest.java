package com.unlimited.sports.globox.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 获取消息列表请求（带分页）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetMessagesRequest {

    /**
     * 消息类型（必填）
     * explore=探索消息, rally=球局消息, system=系统消息
     */
    @NotNull(message = "messageType 不能为空")
    private String messageType;

    /**
     * 页码（从1开始）
     */
    @Min(value = 1, message = "pageNum 最小为 1")
    @Builder.Default
    private Integer pageNum = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "pageSize 最小为 1")
    @Max(value = 100, message = "pageSize 最大为 100")
    @Builder.Default
    private Integer pageSize = 20;
}
