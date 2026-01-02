package com.unlimited.sports.globox.notification.service;

import com.unlimited.sports.globox.notification.dto.callback.PushCallback;

/**
 * 推送回调处理接口
 */
public interface ICallbackService {

    /**
     * 处理推送回调事件
     *
     * @param callback 推送回调数据
     */
    void handlePushCallback(PushCallback callback);
}
