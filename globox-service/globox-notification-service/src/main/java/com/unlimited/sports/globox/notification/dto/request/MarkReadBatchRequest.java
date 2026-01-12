package com.unlimited.sports.globox.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量标记消息已读请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkReadBatchRequest {

    /**
     * 通知ID列表（消息唯一标识，前端从推送数据中获取）
     */
    @NotEmpty(message = "notificationIds 不能为空")
    private List<String> notificationIds;
}
