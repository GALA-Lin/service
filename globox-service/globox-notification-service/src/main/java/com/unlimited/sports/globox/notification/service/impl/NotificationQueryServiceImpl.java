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
import com.unlimited.sports.globox.notification.dto.vo.MessageUserInfo;
import com.unlimited.sports.globox.common.enums.notification.NotificationEntityTypeEnum;
import com.unlimited.sports.globox.common.result.RpcResult;
import com.unlimited.sports.globox.dubbo.user.UserDubboService;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoRequest;
import com.unlimited.sports.globox.dubbo.user.dto.BatchUserInfoResponse;
import com.unlimited.sports.globox.model.auth.vo.UserInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 通知查询服务实现
 */
@Slf4j
@Service
public class NotificationQueryServiceImpl implements INotificationQueryService {

    @Resource
    private IPushRecordsService pushRecordsService;

    @DubboReference(group = "rpc")
    private UserDubboService userDubboService;

    @Override
    public UnreadCountVO getUnreadCount(Long userId) {
        // 查询所有未读消息，按 notification_module 分组统计
        LambdaQueryWrapper<PushRecords> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushRecords::getUserId, userId)
                .eq(PushRecords::getIsRead, 0)
                .notIn(PushRecords::getEventType, MessageTypeEnum.EXCLUDED_EVENT_TYPES);

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
                .notIn(PushRecords::getEventType, MessageTypeEnum.EXCLUDED_EVENT_TYPES)
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
        log.info("request{}",request);
        // 根据消息类型获取模块代码
        MessageTypeEnum messageType = MessageTypeEnum.fromCode(request.getMessageType());
        if (messageType == null) {
            throw new IllegalArgumentException("未知的消息类型: " + request.getMessageType());
        }

        List<Integer> moduleCodes = messageType.getModuleCodes();
        log.info("[消息查询] userId={}, messageType={}, moduleCodes={}", userId, request.getMessageType(), moduleCodes);

        // 构建查询条件
        LambdaQueryWrapper<PushRecords> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PushRecords::getUserId, userId)
                .in(PushRecords::getNotificationModule, moduleCodes)
                .notIn(PushRecords::getEventType, MessageTypeEnum.EXCLUDED_EVENT_TYPES)
                .orderByDesc(PushRecords::getCreatedAt);

        // 分页查询
        Page<PushRecords> page = new Page<>(request.getPageNum(), request.getPageSize());
        IPage<PushRecords> pageResult = pushRecordsService.page(page, wrapper);
        // 转换为 VO
        List<NotificationMessageVO> voList = pageResult.getRecords().stream()
                .map(NotificationMessageVO::fromEntity)
                .collect(Collectors.toList());

        // 附加关联的实体信息
        attachEntityInfo(voList, pageResult.getRecords(), userId);

        return PaginationResult.build(
                voList,
                pageResult.getTotal(),
                request.getPageNum(),
                request.getPageSize()
        );
    }

    /**
     * 为通知消息附加关联的实体信息
     */
    private void attachEntityInfo(List<NotificationMessageVO> voList, List<PushRecords> records, Long userId) {
        // 根据实体类型分组处理
        Map<Integer, List<NotificationMessageVO>> vosByEntityType = voList.stream()
                .filter(vo -> vo.getAttachedEntityType() != null && vo.getAttachedEntityType() != NotificationEntityTypeEnum.NONE.getCode())
                .collect(Collectors.groupingBy(NotificationMessageVO::getAttachedEntityType));

        Map<Long, PushRecords> recordMap = records.stream()
                .collect(Collectors.toMap(PushRecords::getRecordId, r -> r));

        // 处理USER类型的实体
        if (vosByEntityType.containsKey(NotificationEntityTypeEnum.USER.getCode())) {
            enrichUserInfo(vosByEntityType.get(NotificationEntityTypeEnum.USER.getCode()), recordMap, userId);
        }

        // 后续可添加其他实体类型的处理
    }

    /**
     * 为消息富集用户信息
     */
    private void enrichUserInfo(List<NotificationMessageVO> userEntityMessages, Map<Long, PushRecords> recordMap, Long userId) {
        if (CollectionUtils.isEmpty(userEntityMessages)) {
            return;
        }

        try {
            Set<Long> userIds = userEntityMessages.stream()
                    .map(vo -> recordMap.get(vo.getRecordId()))
                    .filter(Objects::nonNull)
                    .map(PushRecords::getAttachedEntityId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (CollectionUtils.isEmpty(userIds)) {
                return;
            }

            BatchUserInfoRequest userRequest = new BatchUserInfoRequest();
            userRequest.setUserIds(new ArrayList<>(userIds));

            RpcResult<BatchUserInfoResponse> rpcResult = userDubboService.batchGetUserInfo(userRequest);

            if (rpcResult.isSuccess() && rpcResult.getData() != null) {
                BatchUserInfoResponse response = rpcResult.getData();
                Map<Long, UserInfoVo> userInfoMap = response.getUsers().stream()
                        .collect(Collectors.toMap(UserInfoVo::getUserId, u -> u));

                // 为每个消息附加对应的用户信息
                userEntityMessages.forEach(vo -> {
                    PushRecords record = recordMap.get(vo.getRecordId());
                    if (record != null && record.getAttachedEntityId() != null) {
                        UserInfoVo userInfo = userInfoMap.get(record.getAttachedEntityId());
                        if (userInfo != null) {
                            MessageUserInfo messageUserInfo = MessageUserInfo.builder()
                                    .userId(userInfo.getUserId())
                                    .nickname(userInfo.getNickName())
                                    .avatarUrl(userInfo.getAvatarUrl())
                                    .build();
                            vo.setEntityInfo(messageUserInfo);
                            log.info("[消息查询] 已附加用户信息: userId={}, attachedUserId={}, nickname={}",
                                    userId, userInfo.getUserId(), userInfo.getNickName());
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("[消息查询] 获取用户信息失败", e);
            // 继续返回消息列表，即使用户信息获取失败
        }
    }
}
