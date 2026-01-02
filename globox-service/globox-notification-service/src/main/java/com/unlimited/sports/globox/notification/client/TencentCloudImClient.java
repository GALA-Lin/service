package com.unlimited.sports.globox.notification.client;

import com.alibaba.fastjson2.JSON;
import com.unlimited.sports.globox.notification.config.TencentCloudImProperties;
import com.unlimited.sports.globox.notification.constants.TencentImErrorCode;
import com.unlimited.sports.globox.notification.dto.request.*;
import com.unlimited.sports.globox.notification.dto.response.PushResponse;
import com.unlimited.sports.globox.notification.dto.response.TencentImResponse;
import com.unlimited.sports.globox.notification.util.UserSigGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 腾讯云IM推送客户端
 * 负责所有腾讯云IM API调用
 */
@Slf4j
@Component
public class TencentCloudImClient {

    @Autowired
    private TencentCloudImProperties properties;

    @Autowired
    private UserSigGenerator userSigGenerator;

    @Autowired
    @Qualifier("tencentImRestTemplate")
    private RestTemplate restTemplate;


    /**
     * 请求基础地址
     */
    private static final String BASE_REQUEST_URL = "https://%s%s?usersig=%s&identifier=%s&sdkappid=%d&random=%d&contenttype=json";

    /**
     * 批量推送（单发/批量）
     * API: /v4/timpush/batch
     *
     * @param request 批量推送请求
     * @return 推送任务ID
     */
    public String batchPush(BatchPushRequest request) {
        log.info("[批量推送] 开始推送: 接收者数量={},接受者={}", request.getToAccount() != null ? request.getToAccount().size() : 0,request.getToAccount() != null ? JSON.toJSONString(request.getToAccount()) : "[]");

        // 设置必填字段
        prepareRequest(request);

        // 发送请求
        String url = buildUrl("/v4/timpush/batch");
        PushResponse response = post(url, request, PushResponse.class);

        if(response == null || !response.isSuccess()) {
            Integer errorCode = response != null ? response.getErrorCode() : null;
            String errorMsg = TencentImErrorCode.getErrorMessage(errorCode);
            String rawErrorInfo = response != null ? response.getErrorInfo() : "Unknown";
            log.error("[批量推送] 推送失败: errorCode={}, 错误描述={}, 原始错误信息={}",
                    errorCode, errorMsg, rawErrorInfo);
            return null;
        }
        return response.getTaskId();

    }

    /**
     * 全员推送
     * API: /v4/timpush/push
     *
     * @param request 全员推送请求
     * @return 推送任务ID
     */
    public String allUserPush(AllUserPushRequest request) {
        log.info("[全员推送] 开始推送");

        // 设置必填字段
        prepareRequest(request);

        // 发送请求
        String url = buildUrl("/v4/timpush/push");
        PushResponse response = post(url, request, PushResponse.class);

        if (response != null && response.isSuccess()) {
            log.info("[全员推送] 推送成功: taskId={}", response.getTaskId());
            return response.getTaskId();
        }

        Integer errorCode = response != null ? response.getErrorCode() : null;
        String errorMsg = TencentImErrorCode.getErrorMessage(errorCode);
        String rawErrorInfo = response != null ? response.getErrorInfo() : "Unknown";
        log.error("[全员推送] 推送失败: errorCode={}, 错误描述={}, 原始错误信息={}",
                errorCode, errorMsg, rawErrorInfo);
        return null;
    }

    /**
     * 条件推送（标签/属性）
     * API: /v4/timpush/push
     *
     * @param request 条件推送请求
     * @return 推送任务ID
     */
    public String conditionalPush(ConditionalPushRequest request) {
        log.info("[条件推送] 开始推送");

        // 设置必填字段
        prepareRequest(request);

        // 发送请求
        String url = buildUrl("/v4/timpush/push");
        PushResponse response = post(url, request, PushResponse.class);

        if (response != null && response.isSuccess()) {
            log.info("[条件推送] 推送成功: taskId={}", response.getTaskId());
            return response.getTaskId();
        }

        Integer errorCode = response != null ? response.getErrorCode() : null;
        String errorMsg = TencentImErrorCode.getErrorMessage(errorCode);
        String rawErrorInfo = response != null ? response.getErrorInfo() : "Unknown";
        log.error("[条件推送] 推送失败: errorCode={}, 错误描述={}, 原始错误信息={}",
                errorCode, errorMsg, rawErrorInfo);
        return null;
    }

    /**
     * 撤回推送
     * API: /v4/timpush/revoke
     *
     * @param request 撤回请求
     * @return 是否成功
     */
    public boolean revokePush(RevokePushRequest request) {
        log.info("[推送撤回] 开始撤回: taskId={}", request.getTaskId());

        // 发送请求
        String url = buildUrl("/v4/timpush/revoke");
        TencentImResponse response = post(url, request, TencentImResponse.class);

        if (response != null && response.isSuccess()) {
            log.info("[推送撤回] 撤回成功: taskId={}", request.getTaskId());
            return true;
        }

        Integer errorCode = response != null ? response.getErrorCode() : null;
        String errorMsg = TencentImErrorCode.getErrorMessage(errorCode);
        String rawErrorInfo = response != null ? response.getErrorInfo() : "Unknown";
        log.error("[推送撤回] 撤回失败: taskId={}, errorCode={}, 错误描述={}, 原始错误信息={}",
                request.getTaskId(), errorCode, errorMsg, rawErrorInfo);
        return false;
    }

    /**
     * 准备请求（设置必填字段）
     *
     * @param request 推送请求
     */
    private void prepareRequest(BasePushRequest request) {
        // 设置 From_Account（管理员账号）
        if (request.getFromAccount() == null) {
            request.setFromAccount(properties.getAdminAccount());
        }

        // 设置 MsgRandom（32位无符号随机数，范围0-4294967295）
        if (request.getMsgRandom() == null) {
            request.setMsgRandom(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE));
        }
    }

    /**
     * 构建请求URL
     *
     * @param endpoint API端点
     * @return 完整URL
     */
    private String buildUrl(String endpoint) {
        // 生成管理员UserSig
        String userSig = userSigGenerator.generateAdminSig();

        // 生成随机数
        long random = ThreadLocalRandom.current().nextLong(0, 4294967296L);

        // 构建URL
        return String.format(BASE_REQUEST_URL,
                properties.getApiDomain(),
                endpoint,
                userSig,
                properties.getAdminAccount(),
                properties.getSdkAppId(),
                random);
    }

    /**
     * 发送POST请求
     *
     * @param url 请求URL
     * @param requestBody 请求体
     * @param responseClass 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    private <T> T post(String url, Object requestBody, Class<T> responseClass) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建请求
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<T> responseEntity = restTemplate.postForEntity(url, entity, responseClass);

            // 返回响应
            return responseEntity.getBody();

        } catch (Exception e) {
            log.error("[HTTP请求失败] url={}", url, e);
            return null;
        }
    }
}
