package com.unlimited.sports.globox.notification.controller;

import com.unlimited.sports.globox.common.result.R;
import com.unlimited.sports.globox.notification.dto.callback.PushCallback;
import com.unlimited.sports.globox.notification.service.ICallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推送回调接收Controller
 * 接收腾讯云推送事件回调
 *
 * 回调事件类型：
 * - EventType=1: 离线推送事件（推送送达）
 * - EventType=2: 在线推送事件
 * - EventType=3: 推送点击事件
 */
@Slf4j
@RestController
@RequestMapping("/notification/callback")
public class PushCallbackController {

    @Autowired
    private ICallbackService callbackService;

    /**
     * 接收腾讯云推送事件回调
     *
     * 回调格式示例：
     * {
     *   "Events": [
     *     {
     *       "CallbackCommand": "Push.AllMemberPush",
     *       "EventType": 1,
     *       "TaskId": "batch_695610d8_5f5fe251_200000e87f01f12_cf7963dc_386ba222",
     *       "TaskTime": 1557481127,
     *       "EventTime": 1557481128,
     *       "To_Account": "user2",
     *       "PushPlatform": 1,
     *       "PushStage": 1,
     *       "ErrCode": 0,
     *       "ErrInfo": "OK"
     *     }
     *   ]
     * }
     *
     * @param callback 推送回调数据
     * @return 处理结果
     */
    @PostMapping("/push-event")
    public R<String> receivePushCallback(@RequestBody PushCallback callback) {
        log.info("[推送回调] 接收到回调请求: 事件数量={}",
                callback.getEvents() != null ? callback.getEvents().size() : 0);

        // 处理回调事件
        callbackService.handlePushCallback(callback);

        return R.ok(null);
    }
}
