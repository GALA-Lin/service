package com.unlimited.sports.globox.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.unlimited.sports.globox.common.enums.notification.PushStatusEnum;
import com.unlimited.sports.globox.model.notification.entity.PushRecords;
import com.unlimited.sports.globox.notification.dto.callback.PushCallback;
import com.unlimited.sports.globox.notification.enums.PushEventType;
import com.unlimited.sports.globox.notification.service.ICallbackService;
import com.unlimited.sports.globox.notification.service.IPushRecordsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 推送回调处理实现
 * 接收并处理腾讯云推送事件回调
 */
@Slf4j
@Service
public class CallbackServiceImpl implements ICallbackService {

    @Autowired
    private IPushRecordsService pushRecordsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePushCallback(PushCallback callback) {
        if (callback == null || callback.getEvents() == null || callback.getEvents().isEmpty()) {
            log.warn("[推送回调] 回调数据为空");
            return;
        }

        log.info("[推送回调] 处理回调事件: 事件数量={}", callback.getEvents().size());

        // 分离送达事件和点击事件
        List<PushCallback.CallbackEvent> deliveryEvents = new ArrayList<>();
        List<PushCallback.CallbackEvent> clickEvents = new ArrayList<>();

        for (PushCallback.CallbackEvent event : callback.getEvents()) {
            PushEventType eventType = PushEventType.fromCode(event.getEventType());
            if (eventType == null) {
                log.warn("[推送回调] 未知的事件类型: eventTypeCode={}", event.getEventType());
                continue;
            }

            if (eventType.isDeliveryEvent()) {
                deliveryEvents.add(event);
            } else if (eventType.isClickEvent()) {
                clickEvents.add(event);
            }
        }

        // 处理送达事件（离线推送和在线推送）
        if (!deliveryEvents.isEmpty()) {
            handleDeliveryEventsBatch(deliveryEvents);
        }

        // 处理点击事件
        if (!clickEvents.isEmpty()) {
            handleClickEventsBatch(clickEvents);
        }

        log.info("[推送回调] 回调处理完成");
    }

    /**
     * 批量处理送达事件（离线推送和在线推送）
     */
    private void handleDeliveryEventsBatch(List<PushCallback.CallbackEvent> events) {

        // 按taskId分组，减少数据库操作
        Map<String, List<PushCallback.CallbackEvent>> eventsByTaskId = events.stream()
                .collect(Collectors.groupingBy(PushCallback.CallbackEvent::getTaskId));

        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<String, List<PushCallback.CallbackEvent>> entry : eventsByTaskId.entrySet()) {
            String taskId = entry.getKey();
            List<PushCallback.CallbackEvent> taskEvents = entry.getValue();

            // 按错误码分类：成功和失败
            Map<Integer, List<PushCallback.CallbackEvent>> eventsByErrCode = taskEvents.stream()
                    .collect(Collectors.groupingBy(e -> e.getErrCode() == 0 ? 0 : 1));

            // 批量更新成功的记录（ErrCode=0）
            List<PushCallback.CallbackEvent> successEvents = eventsByErrCode.getOrDefault(0, new ArrayList<>());
            if (!successEvents.isEmpty()) {
                successCount += updateDeliveryStatus(taskId, successEvents, PushStatusEnum.DELIVERED, null);
            }

            // 批量更新失败的记录（ErrCode != 0）
            List<PushCallback.CallbackEvent> failureEvents = eventsByErrCode.getOrDefault(1, new ArrayList<>());
            if (!failureEvents.isEmpty()) {
                failureCount += updateDeliveryStatus(taskId, failureEvents, PushStatusEnum.FAILED, failureEvents.get(0));
            }
        }

        log.info("[推送回调] 送达事件处理完成: 成功={}, 失败={}", successCount, failureCount);
    }

    /**
     * 批量更新送达状态
     */
    private int updateDeliveryStatus(String taskId, List<PushCallback.CallbackEvent> events,
                                     PushStatusEnum status, PushCallback.CallbackEvent errorEvent) {
        int updateCount = 0;

        for (PushCallback.CallbackEvent event : events) {
            LambdaUpdateWrapper<PushRecords> updateWrapper = Wrappers.lambdaUpdate(PushRecords.class)
                    .eq(PushRecords::getTencentTaskId, taskId)
                    .eq(PushRecords::getDeviceToken, event.getToAccount())
                    .set(PushRecords::getStatus, status)
                    .set(PushRecords::getUpdatedAt, LocalDateTime.now());

            if (errorEvent != null && errorEvent.getErrCode() != 0) {
                updateWrapper
                        .set(PushRecords::getErrorCode, String.valueOf(errorEvent.getErrCode()))
                        .set(PushRecords::getErrorMsg, errorEvent.getErrInfo());
            }

            boolean success = pushRecordsService.update(updateWrapper);
            if (success) {
                updateCount++;
                log.debug("[推送回调] 记录更新成功: taskId={}, toAccount={}, status={}",
                        taskId, event.getToAccount(), status);
            } else {
                log.warn("[推送回调] 未找到对应记录: taskId={}, toAccount={}", taskId, event.getToAccount());
            }
        }

        return updateCount;
    }

    /**
     * 批量处理点击事件
     */
    private void handleClickEventsBatch(List<PushCallback.CallbackEvent> events) {
        log.debug("[推送回调] 批量处理点击事件: 数量={}", events.size());

        int updateCount = 0;

        for (PushCallback.CallbackEvent event : events) {
            String taskId = event.getTaskId();
            String toAccount = event.getToAccount();
            Long eventTime = event.getEventTime();

            // 转换时间戳为LocalDateTime
            LocalDateTime clickTime = Instant.ofEpochSecond(eventTime)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            LambdaUpdateWrapper<PushRecords> updateWrapper = Wrappers.lambdaUpdate(PushRecords.class)
                    .eq(PushRecords::getTencentTaskId, taskId)
                    .eq(PushRecords::getDeviceToken, toAccount)
                    .set(PushRecords::getStatus, PushStatusEnum.CLICKED)
                    .set(PushRecords::getClickedAt, clickTime)
                    .set(PushRecords::getUpdatedAt, LocalDateTime.now());

            boolean success = pushRecordsService.update(updateWrapper);
            if (success) {
                updateCount++;
                log.debug("[推送回调] 点击事件记录更新: taskId={}, toAccount={}, clickTime={}",
                        taskId, toAccount, clickTime);
            } else {
                log.warn("[推送回调] 未找到对应点击记录: taskId={}, toAccount={}", taskId, toAccount);
            }
        }

        log.info("[推送回调] 点击事件处理完成: 更新数量={}", updateCount);
    }
}
