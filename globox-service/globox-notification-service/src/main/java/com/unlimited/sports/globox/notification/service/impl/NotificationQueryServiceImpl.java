package com.unlimited.sports.globox.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unlimited.sports.globox.common.result.PaginationResult;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;
import com.unlimited.sports.globox.notification.dto.request.GetMessagesRequest;
import com.unlimited.sports.globox.notification.dto.request.MarkAllReadRequest;
import com.unlimited.sports.globox.notification.dto.request.MarkReadBatchRequest;
import com.unlimited.sports.globox.notification.dto.vo.NotificationMessageVO;
import com.unlimited.sports.globox.notification.dto.vo.UnreadCountItemVO;
import com.unlimited.sports.globox.notification.dto.vo.UnreadCountVO;
import com.unlimited.sports.globox.notification.enums.MessageTypeEnum;
import com.unlimited.sports.globox.notification.service.INotificationQueryService;
import com.unlimited.sports.globox.notification.service.IPushRecordsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知查询服务实现
 */
@Slf4j
@Service
public class NotificationQueryServiceImpl implements INotificationQueryService {

    @Resource
    private IPushRecordsService pushRecordsService;

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        // 查询所有未读消息，按 notification_module 分组统计
        LambdaQueryWrapper<PushRecords> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushRecords::getUserId, userId)
                .eq(PushRecords::getIsRead, 0);

        List<PushRecords> unreadRecords = pushRecordsService.list(wrapper);

        // 按模块代码分组统计
        Map<Integer, Long> countByModule = unreadRecords.stream()
                .collect(Collectors.groupingBy(
                        PushRecords::getNotificationModule,
                        Collectors.counting()
                ));

        // 构建各消息类型的未读数量项
        List<UnreadCountItemVO> items = new ArrayList<>();
        int totalCount = 0;

        for (MessageTypeEnum messageType : MessageTypeEnum.values()) {
            // 遍历消息类型对应的所有模块代码，累加数量
            int count = messageType.getModuleCodes().stream()
                    .mapToInt(moduleCode -> countByModule.getOrDefault(moduleCode, 0L).intValue())
                    .sum();

            items.add(UnreadCountItemVO.builder()
                    .type(messageType.getCode())
                    .unReadCount(count)
                    .build());
            totalCount += count;
        }

        return UnreadCountVO.builder()
                .items(items)
                .totalUnreadCount(totalCount)
                .build();
    }

    @Override
    @Transactional
    public void markBatchAsRead(MarkReadBatchRequest request, Long userId) {
        LambdaUpdateWrapper<PushRecords> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PushRecords::getUserId, userId)
                .eq(PushRecords::getIsRead, 0)
                .in(PushRecords::getNotificationId, request.getNotificationIds())
                .set(PushRecords::getIsRead, 1)
                .set(PushRecords::getReadAt, LocalDateTime.now());

        pushRecordsService.update(wrapper);
    }

    @Override
    @Transactional
    public void markAllAsRead(MarkAllReadRequest request, Long userId) {
        LambdaUpdateWrapper<PushRecords> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(PushRecords::getUserId, userId)
                .eq(PushRecords::getIsRead, 0)
                .set(PushRecords::getIsRead, 1)
                .set(PushRecords::getReadAt, LocalDateTime.now());

        // 如果指定了消息类型，则只标记该类型的消息
        if (request.getMessageType() != null && !request.getMessageType().isEmpty()) {
            MessageTypeEnum messageType = MessageTypeEnum.fromCode(request.getMessageType());
            if (messageType != null) {
                wrapper.in(PushRecords::getNotificationModule, messageType.getModuleCodes());
            }
        }

        pushRecordsService.update(wrapper);
    }

    @Override
    public PaginationResult<NotificationMessageVO> getMessages(GetMessagesRequest request, Long userId) {
        // 根据消息类型获取模块代码
        MessageTypeEnum messageType = MessageTypeEnum.fromCode(request.getMessageType());
        if (messageType == null) {
            throw new IllegalArgumentException("未知的消息类型: " + request.getMessageType());
        }

        // 构建查询条件
        LambdaQueryWrapper<PushRecords> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushRecords::getUserId, userId)
                .in(PushRecords::getNotificationModule, messageType.getModuleCodes())
                .orderByDesc(PushRecords::getCreatedAt);

        // 分页查询
        Page<PushRecords> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<PushRecords> pageResult = pushRecordsService.page(page, wrapper);

        // 转换为 VO
        List<NotificationMessageVO> voList = pageResult.getRecords().stream()
                .map(NotificationMessageVO::fromEntity)
                .collect(Collectors.toList());

        return PaginationResult.build(
                voList,
                pageResult.getTotal(),
                request.getPageNum(),
                request.getPageSize()
        );
    }
}
