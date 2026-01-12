package com.unlimited.sports.globox.notification.controller;

import com.unlimited.sports.globox.common.constants.RequestHeaderConstants;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.notification.dto.request.GetMessagesRequest;
import com.unlimited.sports.globox.notification.dto.request.MarkAllReadRequest;
import com.unlimited.sports.globox.notification.dto.request.MarkReadBatchRequest;
import com.unlimited.sports.globox.notification.dto.vo.NotificationMessageVO;
import com.unlimited.sports.globox.notification.dto.vo.UnreadCountVO;
import com.unlimited.sports.globox.notification.service.INotificationQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 通知查询Controller
 */
@Slf4j
@RestController
@RequestMapping("/notification/query")
public class NotificationQueryController {

    @Resource
    private INotificationQueryService notificationQueryService;

    /**
     * 获取未读消息数量
     *
     * @param userId 用户ID
     * @return 未读消息数量统计
     */
    @GetMapping("/unread-count")
    public R<UnreadCountVO> getUnreadCount(
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        UnreadCountVO unreadCount = notificationQueryService.getUnreadCount(userId);
        return R.ok(unreadCount);
    }

    /**
     * 批量标记消息已读
     * 前端需要从推送数据的 notificationId 字段获取消息ID，
     * 然后调用此接口标记已读
     *
     * @param request 批量标记请求（包含 notificationIds）
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/mark-read-batch")
    public R<Void> markBatchAsRead(
            @Valid @RequestBody MarkReadBatchRequest request,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        notificationQueryService.markBatchAsRead(request, userId);
        return R.ok();
    }

    /**
     * 全部已读
     *
     * @param request 全部已读请求
     * @param userId 用户ID
     * @return 操作结果
     */
    @PostMapping("/mark-all-read")
    public R<Void> markAllAsRead(
            @RequestBody MarkAllReadRequest request,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        notificationQueryService.markAllAsRead(request, userId);
        return R.ok();
    }

    /**
     * 获取消息列表（分页）
     *
     * @param request 获取消息请求
     * @param userId 用户ID
     * @return 分页消息列表
     */
    @PostMapping("/messages")
    public R<PaginationResult<NotificationMessageVO>> getMessages(
            @Valid @RequestBody GetMessagesRequest request,
            @RequestHeader(RequestHeaderConstants.HEADER_USER_ID) Long userId) {
        PaginationResult<NotificationMessageVO> messages = notificationQueryService.getMessages(request, userId);
        return R.ok(messages);
    }
}
