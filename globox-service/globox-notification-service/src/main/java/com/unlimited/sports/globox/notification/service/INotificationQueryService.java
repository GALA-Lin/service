package com.unlimited.sports.globox.notification.service;

import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.notification.dto.request.GetMessagesRequest;
import com.unlimited.sports.globox.notification.dto.request.MarkAllReadRequest;
import com.unlimited.sports.globox.notification.dto.request.MarkReadBatchRequest;
import com.unlimited.sports.globox.notification.dto.vo.NotificationMessageVO;
import com.unlimited.sports.globox.notification.dto.vo.UnreadCountVO;

/**
 * 通知查询服务接口
 */
public interface INotificationQueryService {

    /**
     * 获取未读消息数量
     *
     * @param userId 用户ID
     * @return 未读消息数量统计
     */
    UnreadCountVO getUnreadCount(Long userId);

    /**
     * 批量标记消息已读
     *
     * @param request 批量标记请求
     * @param userId 用户ID
     */
    void markBatchAsRead(MarkReadBatchRequest request, Long userId);

    /**
     * 全部已读
     *
     * @param request 全部已读请求
     * @param userId 用户ID
     */
    void markAllAsRead(MarkAllReadRequest request, Long userId);

    /**
     * 获取消息列表（分页）
     *
     * @param request 获取消息请求
     * @param userId 用户ID
     * @return 分页消息列表
     */
    PaginationResult<NotificationMessageVO> getMessages(GetMessagesRequest request, Long userId);
}
